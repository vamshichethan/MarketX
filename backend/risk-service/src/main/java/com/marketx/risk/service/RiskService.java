package com.marketx.risk.service;

import com.marketx.risk.client.PnlClient;
import com.marketx.risk.client.PositionClient;
import com.marketx.risk.client.PositionClient.PositionLookupResult;
import com.marketx.risk.dto.EvaluateRiskRequest;
import com.marketx.risk.dto.MarketPriceRequest;
import com.marketx.risk.dto.PositionResponse;
import com.marketx.risk.dto.RiskDecisionResponse;
import com.marketx.risk.dto.RiskEvaluationResponse;
import com.marketx.risk.entity.MarketPriceEntity;
import com.marketx.risk.entity.RiskDecisionEntity;
import com.marketx.risk.entity.RiskLimitEntity;
import com.marketx.risk.enums.OrderSide;
import com.marketx.risk.enums.OrderType;
import com.marketx.risk.enums.RiskDecision;
import com.marketx.risk.exception.InvalidRiskRequestException;
import com.marketx.risk.repository.MarketPriceRepository;
import com.marketx.risk.repository.RiskDecisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RiskService {
    private static final String REASON_SEPARATOR = "; ";

    private final AtomicLong nextDecisionSequence = new AtomicLong(1);
    private final RiskLimitService riskLimitService;
    private final RiskDecisionRepository riskDecisionRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final PositionClient positionClient;
    private final PnlClient pnlClient;

    public RiskService(
            RiskLimitService riskLimitService,
            RiskDecisionRepository riskDecisionRepository,
            MarketPriceRepository marketPriceRepository,
            PositionClient positionClient,
            PnlClient pnlClient
    ) {
        this.riskLimitService = riskLimitService;
        this.riskDecisionRepository = riskDecisionRepository;
        this.marketPriceRepository = marketPriceRepository;
        this.positionClient = positionClient;
        this.pnlClient = pnlClient;
    }

    @Transactional
    public RiskEvaluationResponse evaluate(EvaluateRiskRequest request) {
        validateEvaluateRequest(request);

        String accountId = request.accountId().toUpperCase();
        String symbol = request.symbol().toUpperCase();
        RiskLimitEntity limits = riskLimitService.getOrCreateDefaults(accountId);
        List<String> reasons = new java.util.ArrayList<>();

        checkMaxOrderQuantity(request, limits, reasons);
        checkPositionLimit(request, accountId, symbol, limits, reasons);
        checkExposure(request, symbol, limits, reasons);
        checkDailyLoss(accountId, limits, reasons);

        RiskDecision decision = reasons.isEmpty() ? RiskDecision.APPROVED : RiskDecision.REJECTED;
        RiskEvaluationResponse response = new RiskEvaluationResponse(
                decision == RiskDecision.APPROVED,
                decision,
                reasons,
                accountId,
                symbol
        );
        saveDecision(request, response);
        return response;
    }

    @Transactional
    public void updateMarketPrice(MarketPriceRequest request) {
        validateMarketPrice(request);
        String symbol = request.symbol().toUpperCase();

        MarketPriceEntity marketPrice = marketPriceRepository.findBySymbol(symbol).orElseGet(MarketPriceEntity::new);
        marketPrice.setSymbol(symbol);
        marketPrice.setPrice(request.price());
        marketPrice.setUpdatedAt(request.timestamp() == null ? LocalDateTime.now() : request.timestamp());
        marketPriceRepository.save(marketPrice);
    }

    public List<RiskDecisionResponse> getDecisions(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new InvalidRiskRequestException("accountId is required");
        }

        return riskDecisionRepository.findByAccountIdOrderByCheckedAtDesc(accountId.toUpperCase()).stream()
                .map(this::toDecisionResponse)
                .toList();
    }

    private void checkMaxOrderQuantity(
            EvaluateRiskRequest request,
            RiskLimitEntity limits,
            List<String> reasons
    ) {
        if (request.quantity() > limits.getMaxOrderQuantity()) {
            reasons.add("Order quantity exceeds max allowed quantity");
        }
    }

    private void checkPositionLimit(
            EvaluateRiskRequest request,
            String accountId,
            String symbol,
            RiskLimitEntity limits,
            List<String> reasons
    ) {
        PositionLookupResult positionLookup = positionClient.getPosition(accountId, symbol);
        if (!positionLookup.available()) {
            reasons.add("Position unavailable for position limit check");
            return;
        }

        int currentPosition = positionLookup.position().map(PositionResponse::netQuantity).orElse(0);
        int signedQuantity = request.side() == OrderSide.BUY ? request.quantity() : -request.quantity();
        int projectedPosition = currentPosition + signedQuantity;

        if (Math.abs(projectedPosition) > limits.getMaxPositionQuantity()) {
            reasons.add("Position limit exceeded");
        }
    }

    private void checkExposure(
            EvaluateRiskRequest request,
            String symbol,
            RiskLimitEntity limits,
            List<String> reasons
    ) {
        Optional<BigDecimal> price = priceForExposure(request, symbol);
        if (price.isEmpty()) {
            reasons.add("Market price unavailable for exposure check");
            return;
        }

        BigDecimal exposure = BigDecimal.valueOf(request.quantity()).multiply(price.get());
        if (exposure.compareTo(limits.getMaxExposure()) > 0) {
            reasons.add("Exposure limit exceeded");
        }
    }

    private void checkDailyLoss(String accountId, RiskLimitEntity limits, List<String> reasons) {
        Optional<BigDecimal> totalPnl = pnlClient.getTotalPnlForAccount(accountId);
        if (totalPnl.isEmpty()) {
            reasons.add("PnL unavailable for daily loss check");
            return;
        }

        BigDecimal maxLossThreshold = limits.getMaxDailyLoss().negate();
        if (totalPnl.get().compareTo(maxLossThreshold) <= 0) {
            reasons.add("Daily loss limit breached");
        }
    }

    private Optional<BigDecimal> priceForExposure(EvaluateRiskRequest request, String symbol) {
        if (request.type() == OrderType.LIMIT) {
            return Optional.ofNullable(request.price());
        }

        return marketPriceRepository.findBySymbol(symbol).map(MarketPriceEntity::getPrice);
    }

    private void saveDecision(EvaluateRiskRequest request, RiskEvaluationResponse response) {
        RiskDecisionEntity entity = new RiskDecisionEntity();
        entity.setDecisionId(nextDecisionId());
        entity.setAccountId(response.accountId());
        entity.setSymbol(response.symbol());
        entity.setSide(request.side());
        entity.setOrderType(request.type());
        entity.setQuantity(request.quantity());
        entity.setPrice(request.price());
        entity.setApproved(response.approved());
        entity.setDecision(response.decision());
        entity.setReasons(String.join(REASON_SEPARATOR, response.reasons()));
        entity.setCheckedAt(LocalDateTime.now());
        riskDecisionRepository.save(entity);
    }

    private String nextDecisionId() {
        return "RSK-" + nextDecisionSequence.getAndIncrement();
    }

    private void validateEvaluateRequest(EvaluateRiskRequest request) {
        if (request.accountId() == null || request.accountId().isBlank()) {
            throw new InvalidRiskRequestException("accountId is required");
        }
        if (request.symbol() == null || request.symbol().isBlank()) {
            throw new InvalidRiskRequestException("symbol is required");
        }
        if (request.side() == null) {
            throw new InvalidRiskRequestException("side is required");
        }
        if (request.type() == null) {
            throw new InvalidRiskRequestException("type is required");
        }
        if (request.quantity() <= 0) {
            throw new InvalidRiskRequestException("quantity must be greater than zero");
        }
        if (request.type() == OrderType.LIMIT
                && (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new InvalidRiskRequestException("LIMIT order requires price greater than zero");
        }
        if (request.type() == OrderType.MARKET
                && request.price() != null
                && request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRiskRequestException("Market order price cannot be negative");
        }
    }

    private void validateMarketPrice(MarketPriceRequest request) {
        if (request.symbol() == null || request.symbol().isBlank()) {
            throw new InvalidRiskRequestException("symbol is required");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRiskRequestException("market price must be greater than zero");
        }
    }

    private RiskDecisionResponse toDecisionResponse(RiskDecisionEntity entity) {
        return new RiskDecisionResponse(
                entity.getDecisionId(),
                entity.getAccountId(),
                entity.getSymbol(),
                entity.getSide(),
                entity.getOrderType(),
                entity.getQuantity(),
                entity.getPrice(),
                entity.isApproved(),
                entity.getDecision(),
                parseReasons(entity.getReasons()),
                entity.getCheckedAt()
        );
    }

    private List<String> parseReasons(String reasons) {
        if (reasons == null || reasons.isBlank()) {
            return List.of();
        }
        return Arrays.asList(reasons.split(REASON_SEPARATOR));
    }
}
