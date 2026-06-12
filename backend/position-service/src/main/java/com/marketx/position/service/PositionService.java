package com.marketx.position.service;

import com.marketx.position.dto.PositionResponse;
import com.marketx.position.dto.PositionUpdateResult;
import com.marketx.position.dto.TradeEventRequest;
import com.marketx.position.entity.PositionEntity;
import com.marketx.position.entity.ProcessedTradeEntity;
import com.marketx.position.enums.OrderSide;
import com.marketx.position.enums.PositionType;
import com.marketx.position.exception.DuplicateTradeException;
import com.marketx.position.exception.InvalidTradeEventException;
import com.marketx.position.exception.PositionNotFoundException;
import com.marketx.position.repository.PositionRepository;
import com.marketx.position.repository.ProcessedTradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PositionService {
    private static final int PRICE_SCALE = 6;

    private final PositionRepository positionRepository;
    private final ProcessedTradeRepository processedTradeRepository;

    public PositionService(
            PositionRepository positionRepository,
            ProcessedTradeRepository processedTradeRepository
    ) {
        this.positionRepository = positionRepository;
        this.processedTradeRepository = processedTradeRepository;
    }

    @Transactional
    public PositionUpdateResult processTrade(TradeEventRequest request) {
        validateTradeEvent(request);

        if (processedTradeRepository.existsByTradeId(request.tradeId())) {
            throw new DuplicateTradeException(request.tradeId());
        }

        String accountId = request.accountId().toUpperCase();
        String symbol = request.symbol().toUpperCase();
        boolean created = positionRepository.findByAccountIdAndSymbol(accountId, symbol).isEmpty();
        PositionEntity position = positionRepository
                .findByAccountIdAndSymbol(accountId, symbol)
                .orElseGet(() -> newPosition(accountId, symbol));

        applyTrade(position, request);
        PositionEntity savedPosition = positionRepository.save(position);
        markTradeProcessed(request, accountId, symbol);
        return new PositionUpdateResult(toResponse(savedPosition), created);
    }

    public PositionResponse getPosition(String accountId, String symbol) {
        return positionRepository.findByAccountIdAndSymbol(accountId.toUpperCase(), symbol.toUpperCase())
                .map(this::toResponse)
                .orElseThrow(() -> new PositionNotFoundException(accountId, symbol));
    }

    public List<PositionResponse> getPositionsForAccount(String accountId) {
        return positionRepository.findByAccountId(accountId.toUpperCase()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PositionResponse> getAllPositions() {
        return positionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private PositionEntity newPosition(String accountId, String symbol) {
        PositionEntity position = new PositionEntity();
        LocalDateTime now = LocalDateTime.now();
        position.setAccountId(accountId);
        position.setSymbol(symbol);
        position.setNetQuantity(0);
        position.setAveragePrice(BigDecimal.ZERO);
        position.setPositionType(PositionType.FLAT);
        position.setCreatedAt(now);
        position.setUpdatedAt(now);
        return position;
    }

    private void applyTrade(PositionEntity position, TradeEventRequest request) {
        int oldQuantity = position.getNetQuantity();
        BigDecimal oldAveragePrice = position.getAveragePrice();
        int signedTradeQuantity = request.side() == OrderSide.BUY ? request.quantity() : -request.quantity();
        int newQuantity = oldQuantity + signedTradeQuantity;

        BigDecimal newAveragePrice = calculateAveragePrice(
                oldQuantity,
                oldAveragePrice,
                request.side(),
                request.quantity(),
                request.price(),
                newQuantity
        );

        position.setNetQuantity(newQuantity);
        position.setAveragePrice(newAveragePrice);
        position.setPositionType(toPositionType(newQuantity));
        position.setUpdatedAt(LocalDateTime.now());
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

        // Selling reduces a long position. If the sell flips long to short, the new short average starts at sell price.
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

        // Buying reduces a short position. If the buy flips short to long, the new long average starts at buy price.
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

    private PositionResponse toResponse(PositionEntity entity) {
        return new PositionResponse(
                entity.getAccountId(),
                entity.getSymbol(),
                entity.getNetQuantity(),
                entity.getAveragePrice(),
                entity.getPositionType(),
                entity.getUpdatedAt()
        );
    }
}
