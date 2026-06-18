package com.marketx.pnl.service;

import com.marketx.pnl.dto.MarketPriceUpdateRequest;
import com.marketx.pnl.dto.PnlResponse;
import com.marketx.pnl.dto.PnlUpdateResult;
import com.marketx.pnl.dto.TradeEventRequest;
import com.marketx.pnl.entity.MarketPriceEntity;
import com.marketx.pnl.entity.PnlEntity;
import com.marketx.pnl.entity.ProcessedTradeEntity;
import com.marketx.pnl.enums.OrderSide;
import com.marketx.pnl.enums.PositionType;
import com.marketx.pnl.exception.DuplicateTradeException;
import com.marketx.pnl.exception.InvalidTradeEventException;
import com.marketx.pnl.exception.PnlNotFoundException;
import com.marketx.pnl.metrics.PnlMetricsService;
import com.marketx.pnl.repository.MarketPriceRepository;
import com.marketx.pnl.repository.PnlRepository;
import com.marketx.pnl.repository.ProcessedTradeRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PnlService {
    private static final Logger log = LoggerFactory.getLogger(PnlService.class);
    private static final int PRICE_SCALE = 6;

    private final PnlRepository pnlRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final ProcessedTradeRepository processedTradeRepository;
    private final PnlMetricsService metricsService;

    public PnlService(
            PnlRepository pnlRepository,
            MarketPriceRepository marketPriceRepository,
            ProcessedTradeRepository processedTradeRepository,
            PnlMetricsService metricsService
    ) {
        this.pnlRepository = pnlRepository;
        this.marketPriceRepository = marketPriceRepository;
        this.processedTradeRepository = processedTradeRepository;
        this.metricsService = metricsService;
    }

    @Transactional
    public PnlUpdateResult processTrade(TradeEventRequest request) {
        Timer.Sample sample = metricsService.startCalculation();
        validateTradeEvent(request);

        if (processedTradeRepository.existsByTradeId(request.tradeId())) {
            throw new DuplicateTradeException(request.tradeId());
        }

        String accountId = request.accountId().toUpperCase();
        String symbol = request.symbol().toUpperCase();
        boolean created = pnlRepository.findByAccountIdAndSymbol(accountId, symbol).isEmpty();
        PnlEntity pnl = pnlRepository.findByAccountIdAndSymbol(accountId, symbol)
                .orElseGet(() -> newPnl(accountId, symbol));

        applyTrade(pnl, request);
        marketPriceRepository.findBySymbol(symbol).ifPresent(price -> {
            pnl.setLastMarketPrice(price.getPrice());
            recalculateUnrealizedPnl(pnl);
        });
        recalculateTotalPnl(pnl);

        PnlEntity savedPnl = pnlRepository.save(pnl);
        markTradeProcessed(request, accountId, symbol);
        log.info("PnL updated accountId={} symbol={} tradeId={} realizedPnl={} unrealizedPnl={} totalPnl={}",
                accountId,
                symbol,
                request.tradeId(),
                savedPnl.getRealizedPnl(),
                savedPnl.getUnrealizedPnl(),
                savedPnl.getTotalPnl());
        metricsService.recordUpdate(sample);
        return new PnlUpdateResult(toResponse(savedPnl), created);
    }

    @Transactional
    public List<PnlResponse> updateMarketPrice(MarketPriceUpdateRequest request) {
        Timer.Sample sample = metricsService.startCalculation();
        validateMarketPrice(request);
        String symbol = request.symbol().toUpperCase();

        MarketPriceEntity marketPrice = marketPriceRepository.findBySymbol(symbol).orElseGet(MarketPriceEntity::new);
        marketPrice.setSymbol(symbol);
        marketPrice.setPrice(request.price());
        marketPrice.setUpdatedAt(request.timestamp());
        marketPriceRepository.save(marketPrice);

        List<PnlEntity> rows = pnlRepository.findBySymbol(symbol);
        for (PnlEntity pnl : rows) {
            pnl.setLastMarketPrice(request.price());
            recalculateUnrealizedPnl(pnl);
            recalculateTotalPnl(pnl);
            pnl.setUpdatedAt(LocalDateTime.now());
        }

        List<PnlResponse> responses = pnlRepository.saveAll(rows).stream()
                .map(this::toResponse)
                .toList();
        log.info("PnL market price updated symbol={} price={} impactedRows={}", symbol, request.price(), responses.size());
        if (responses.isEmpty()) {
            metricsService.recordCalculationLatency(sample);
        } else {
            metricsService.recordUpdates(sample, responses.size());
        }
        return responses;
    }

    public PnlResponse getPnl(String accountId, String symbol) {
        return pnlRepository.findByAccountIdAndSymbol(accountId.toUpperCase(), symbol.toUpperCase())
                .map(this::toResponse)
                .orElseThrow(() -> new PnlNotFoundException(accountId, symbol));
    }

    public List<PnlResponse> getPnlForAccount(String accountId) {
        return pnlRepository.findByAccountId(accountId.toUpperCase()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PnlResponse> getAllPnl() {
        return pnlRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private PnlEntity newPnl(String accountId, String symbol) {
        LocalDateTime now = LocalDateTime.now();
        PnlEntity pnl = new PnlEntity();
        pnl.setAccountId(accountId);
        pnl.setSymbol(symbol);
        pnl.setNetQuantity(0);
        pnl.setAveragePrice(BigDecimal.ZERO);
        pnl.setRealizedPnl(BigDecimal.ZERO);
        pnl.setUnrealizedPnl(BigDecimal.ZERO);
        pnl.setTotalPnl(BigDecimal.ZERO);
        pnl.setLastMarketPrice(BigDecimal.ZERO);
        pnl.setPositionType(PositionType.FLAT);
        pnl.setCreatedAt(now);
        pnl.setUpdatedAt(now);
        return pnl;
    }

    private void applyTrade(PnlEntity pnl, TradeEventRequest request) {
        int oldQuantity = pnl.getNetQuantity();
        BigDecimal oldAveragePrice = pnl.getAveragePrice();
        int signedTradeQuantity = request.side() == OrderSide.BUY ? request.quantity() : -request.quantity();
        int newQuantity = oldQuantity + signedTradeQuantity;

        BigDecimal realizedDelta = calculateRealizedPnl(
                oldQuantity,
                oldAveragePrice,
                request.side(),
                request.quantity(),
                request.price()
        );
        BigDecimal newAveragePrice = calculateAveragePrice(
                oldQuantity,
                oldAveragePrice,
                request.side(),
                request.quantity(),
                request.price(),
                newQuantity
        );

        pnl.setNetQuantity(newQuantity);
        pnl.setAveragePrice(newAveragePrice);
        pnl.setRealizedPnl(pnl.getRealizedPnl().add(realizedDelta));
        pnl.setPositionType(toPositionType(newQuantity));
        pnl.setUpdatedAt(LocalDateTime.now());
    }

    private BigDecimal calculateRealizedPnl(
            int oldQuantity,
            BigDecimal oldAveragePrice,
            OrderSide side,
            int tradeQuantity,
            BigDecimal tradePrice
    ) {
        if (oldQuantity > 0 && side == OrderSide.SELL) {
            int closedQuantity = Math.min(oldQuantity, tradeQuantity);
            return tradePrice.subtract(oldAveragePrice).multiply(BigDecimal.valueOf(closedQuantity));
        }

        if (oldQuantity < 0 && side == OrderSide.BUY) {
            int closedQuantity = Math.min(Math.abs(oldQuantity), tradeQuantity);
            return oldAveragePrice.subtract(tradePrice).multiply(BigDecimal.valueOf(closedQuantity));
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calculateAveragePrice(
            int oldQuantity,
            BigDecimal oldAveragePrice,
            OrderSide side,
            int tradeQuantity,
            BigDecimal tradePrice,
            int newQuantity
    ) {
        if (newQuantity == 0) {
            return BigDecimal.ZERO;
        }

        if (oldQuantity == 0) {
            return tradePrice;
        }

        if (oldQuantity > 0) {
            return calculateFromLong(oldQuantity, oldAveragePrice, side, tradeQuantity, tradePrice, newQuantity);
        }

        return calculateFromShort(oldQuantity, oldAveragePrice, side, tradeQuantity, tradePrice, newQuantity);
    }

    private BigDecimal calculateFromLong(
            int oldQuantity,
            BigDecimal oldAveragePrice,
            OrderSide side,
            int tradeQuantity,
            BigDecimal tradePrice,
            int newQuantity
    ) {
        if (side == OrderSide.BUY) {
            return weightedAverage(oldQuantity, oldAveragePrice, tradeQuantity, tradePrice, newQuantity);
        }

        // Selling reduces a long. If the sell flips long to short, the new short average starts at sell price.
        if (newQuantity < 0) {
            return tradePrice;
        }

        return oldAveragePrice;
    }

    private BigDecimal calculateFromShort(
            int oldQuantity,
            BigDecimal oldAveragePrice,
            OrderSide side,
            int tradeQuantity,
            BigDecimal tradePrice,
            int newQuantity
    ) {
        if (side == OrderSide.SELL) {
            return weightedAverage(Math.abs(oldQuantity), oldAveragePrice, tradeQuantity, tradePrice, Math.abs(newQuantity));
        }

        // Buying reduces a short. If the buy flips short to long, the new long average starts at buy price.
        if (newQuantity > 0) {
            return tradePrice;
        }

        return oldAveragePrice;
    }

    private BigDecimal weightedAverage(
            int oldAbsoluteQuantity,
            BigDecimal oldAveragePrice,
            int tradeQuantity,
            BigDecimal tradePrice,
            int newAbsoluteQuantity
    ) {
        BigDecimal oldValue = oldAveragePrice.multiply(BigDecimal.valueOf(oldAbsoluteQuantity));
        BigDecimal tradeValue = tradePrice.multiply(BigDecimal.valueOf(tradeQuantity));
        return oldValue.add(tradeValue)
                .divide(BigDecimal.valueOf(Math.abs(newAbsoluteQuantity)), PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private void recalculateUnrealizedPnl(PnlEntity pnl) {
        BigDecimal marketPrice = pnl.getLastMarketPrice();
        if (marketPrice == null || marketPrice.compareTo(BigDecimal.ZERO) == 0 || pnl.getNetQuantity() == 0) {
            pnl.setUnrealizedPnl(BigDecimal.ZERO);
            return;
        }

        if (pnl.getNetQuantity() > 0) {
            pnl.setUnrealizedPnl(
                    marketPrice.subtract(pnl.getAveragePrice()).multiply(BigDecimal.valueOf(pnl.getNetQuantity()))
            );
            return;
        }

        pnl.setUnrealizedPnl(
                pnl.getAveragePrice().subtract(marketPrice).multiply(BigDecimal.valueOf(Math.abs(pnl.getNetQuantity())))
        );
    }

    private void recalculateTotalPnl(PnlEntity pnl) {
        pnl.setTotalPnl(pnl.getRealizedPnl().add(pnl.getUnrealizedPnl()));
    }

    private PositionType toPositionType(int netQuantity) {
        if (netQuantity > 0) {
            return PositionType.LONG;
        }

        if (netQuantity < 0) {
            return PositionType.SHORT;
        }

        return PositionType.FLAT;
    }

    private void markTradeProcessed(TradeEventRequest request, String accountId, String symbol) {
        ProcessedTradeEntity processedTrade = new ProcessedTradeEntity();
        processedTrade.setTradeId(request.tradeId());
        processedTrade.setAccountId(accountId);
        processedTrade.setSymbol(symbol);
        processedTrade.setProcessedAt(LocalDateTime.now());
        processedTradeRepository.save(processedTrade);
    }

    private void validateTradeEvent(TradeEventRequest request) {
        if (request.tradeId() == null || request.tradeId().isBlank()) {
            throw new InvalidTradeEventException("tradeId is required");
        }
        if (request.accountId() == null || request.accountId().isBlank()) {
            throw new InvalidTradeEventException("accountId is required");
        }
        if (request.symbol() == null || request.symbol().isBlank()) {
            throw new InvalidTradeEventException("symbol is required");
        }
        if (request.side() == null) {
            throw new InvalidTradeEventException("side is required");
        }
        if (request.quantity() <= 0) {
            throw new InvalidTradeEventException("quantity must be greater than zero");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTradeEventException("price must be greater than zero");
        }
        if (request.executedAt() == null) {
            throw new InvalidTradeEventException("executedAt is required");
        }
    }

    private void validateMarketPrice(MarketPriceUpdateRequest request) {
        if (request.symbol() == null || request.symbol().isBlank()) {
            throw new InvalidTradeEventException("symbol is required");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTradeEventException("market price must be greater than zero");
        }
        if (request.timestamp() == null) {
            throw new InvalidTradeEventException("timestamp is required");
        }
    }

    private PnlResponse toResponse(PnlEntity entity) {
        return new PnlResponse(
                entity.getAccountId(),
                entity.getSymbol(),
                entity.getNetQuantity(),
                entity.getAveragePrice(),
                entity.getRealizedPnl(),
                entity.getUnrealizedPnl(),
                entity.getTotalPnl(),
                entity.getLastMarketPrice(),
                entity.getPositionType(),
                entity.getUpdatedAt()
        );
    }
}
