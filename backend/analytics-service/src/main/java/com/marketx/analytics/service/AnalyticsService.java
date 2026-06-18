package com.marketx.analytics.service;

import com.marketx.analytics.dto.AnalyticsResponse;
import com.marketx.analytics.dto.DashboardResponse;
import com.marketx.analytics.dto.MarketDataEvent;
import com.marketx.analytics.dto.MarketPriceEvent;
import com.marketx.analytics.dto.SymbolDashboardRow;
import com.marketx.analytics.dto.TradeExecutedEvent;
import com.marketx.analytics.entity.ProcessedEventEntity;
import com.marketx.analytics.entity.SymbolAnalyticsEntity;
import com.marketx.analytics.exception.AnalyticsNotFoundException;
import com.marketx.analytics.metrics.AnalyticsMetricsService;
import com.marketx.analytics.repository.ProcessedEventRepository;
import com.marketx.analytics.repository.SymbolAnalyticsRepository;
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
public class AnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final int MONEY_SCALE = 2;
    private static final int VALUE_SCALE = 6;

    private final SymbolAnalyticsRepository analyticsRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final AnalyticsMetricsService metricsService;

    public AnalyticsService(
            SymbolAnalyticsRepository analyticsRepository,
            ProcessedEventRepository processedEventRepository,
            AnalyticsMetricsService metricsService
    ) {
        this.analyticsRepository = analyticsRepository;
        this.processedEventRepository = processedEventRepository;
        this.metricsService = metricsService;
    }

    @Transactional
    public AnalyticsResponse processMarketData(MarketDataEvent event) {
        Timer.Sample sample = metricsService.startUpdate();
        try {
            validateMarketData(event);
            if (skipProcessed(event.eventId(), "MarketDataEvent")) {
                return toResponse(getOrCreate(event.symbol()));
            }

            String symbol = normalizeSymbol(event.symbol());
            SymbolAnalyticsEntity analytics = getOrCreate(symbol);
            analytics.setLatestPrice(scaleMoney(event.price()));
            analytics.setBidPrice(scaleMoney(event.bidPrice()));
            analytics.setAskPrice(scaleMoney(event.askPrice()));
            analytics.setSpread(scaleMoney(resolveSpread(event.bidPrice(), event.askPrice(), event.spread())));
            analytics.setLastUpdatedAt(event.timestamp() == null ? LocalDateTime.now() : event.timestamp());

            SymbolAnalyticsEntity saved = analyticsRepository.save(analytics);
            markProcessed(event.eventId(), "MarketDataEvent");
            return toResponse(saved);
        } finally {
            metricsService.recordUpdate(sample);
        }
    }

    @Transactional
    public AnalyticsResponse processMarketPrice(MarketPriceEvent event) {
        Timer.Sample sample = metricsService.startUpdate();
        try {
            validateMarketPrice(event);
            if (skipProcessed(event.eventId(), "MarketPriceEvent")) {
                return toResponse(getOrCreate(event.symbol()));
            }

            SymbolAnalyticsEntity analytics = getOrCreate(event.symbol());
            analytics.setLatestPrice(scaleMoney(event.price()));
            analytics.setLastUpdatedAt(event.timestamp() == null ? LocalDateTime.now() : event.timestamp());

            SymbolAnalyticsEntity saved = analyticsRepository.save(analytics);
            markProcessed(event.eventId(), "MarketPriceEvent");
            return toResponse(saved);
        } finally {
            metricsService.recordUpdate(sample);
        }
    }

    @Transactional
    public AnalyticsResponse processTrade(TradeExecutedEvent event) {
        Timer.Sample sample = metricsService.startUpdate();
        try {
            validateTrade(event);
            if (skipProcessed(event.eventId(), "TradeExecutedEvent")) {
                return toResponse(getOrCreate(event.symbol()));
            }

            SymbolAnalyticsEntity analytics = getOrCreate(event.symbol());
            long newTotalVolume = analytics.getTotalVolume() + event.quantity();
            BigDecimal tradedValue = event.price().multiply(BigDecimal.valueOf(event.quantity()));
            BigDecimal newTotalTradedValue = analytics.getTotalTradedValue().add(tradedValue);
            BigDecimal newVwap = newTotalTradedValue.divide(BigDecimal.valueOf(newTotalVolume), MONEY_SCALE, RoundingMode.HALF_UP);

            analytics.setTotalVolume(newTotalVolume);
            analytics.setTotalTradedValue(newTotalTradedValue.setScale(VALUE_SCALE, RoundingMode.HALF_UP));
            analytics.setVwap(newVwap);
            analytics.setTradeCount(analytics.getTradeCount() + 1);
            analytics.setLastUpdatedAt(event.executedAt() == null ? LocalDateTime.now() : event.executedAt());

            SymbolAnalyticsEntity saved = analyticsRepository.save(analytics);
            markProcessed(event.eventId(), "TradeExecutedEvent");
            return toResponse(saved);
        } finally {
            metricsService.recordUpdate(sample);
        }
    }

    public AnalyticsResponse getAnalytics(String symbol) {
        return analyticsRepository.findBySymbol(normalizeSymbol(symbol))
                .map(this::toResponse)
                .orElseThrow(() -> new AnalyticsNotFoundException(symbol));
    }

    public List<AnalyticsResponse> getAllAnalytics() {
        return analyticsRepository.findAllByOrderBySymbolAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public DashboardResponse getDashboard() {
        List<SymbolDashboardRow> rows = analyticsRepository.findAllByOrderBySymbolAsc().stream()
                .map(entity -> new SymbolDashboardRow(
                        entity.getSymbol(),
                        entity.getVwap(),
                        entity.getTotalVolume(),
                        entity.getSpread(),
                        entity.getTradeCount()
                ))
                .toList();
        return new DashboardResponse(rows);
    }

    @Transactional
    public void resetSymbol(String symbol) {
        analyticsRepository.deleteBySymbol(normalizeSymbol(symbol));
    }

    @Transactional
    public void resetAll() {
        analyticsRepository.deleteAll();
    }

    private SymbolAnalyticsEntity getOrCreate(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        return analyticsRepository.findBySymbol(normalizedSymbol)
                .orElseGet(() -> newAnalytics(normalizedSymbol));
    }

    private SymbolAnalyticsEntity newAnalytics(String symbol) {
        SymbolAnalyticsEntity entity = new SymbolAnalyticsEntity();
        entity.setSymbol(symbol);
        entity.setTotalVolume(0L);
        entity.setTotalTradedValue(BigDecimal.ZERO.setScale(VALUE_SCALE, RoundingMode.HALF_UP));
        entity.setVwap(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        entity.setLatestPrice(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        entity.setBidPrice(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        entity.setAskPrice(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        entity.setSpread(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        entity.setTradeCount(0L);
        entity.setLastUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private boolean skipProcessed(String eventId, String eventType) {
        boolean processed = processedEventRepository.existsByEventId(eventId);
        if (processed) {
            log.info("Skipping duplicate {} eventId={}", eventType, eventId);
        }
        return processed;
    }

    private void markProcessed(String eventId, String eventType) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(eventId);
        entity.setEventType(eventType);
        entity.setProcessedAt(LocalDateTime.now());
        processedEventRepository.save(entity);
    }

    private AnalyticsResponse toResponse(SymbolAnalyticsEntity entity) {
        return new AnalyticsResponse(
                entity.getSymbol(),
                entity.getVwap(),
                entity.getTotalVolume(),
                entity.getSpread(),
                entity.getTradeCount(),
                entity.getLatestPrice(),
                entity.getBidPrice(),
                entity.getAskPrice(),
                entity.getLastUpdatedAt()
        );
    }

    private BigDecimal resolveSpread(BigDecimal bidPrice, BigDecimal askPrice, BigDecimal spread) {
        if (spread != null) {
            return spread;
        }
        return askPrice.subtract(bidPrice);
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }

    private void validateMarketData(MarketDataEvent event) {
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (event.symbol() == null || event.symbol().isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        requirePositive(event.price(), "price");
        requirePositive(event.bidPrice(), "bidPrice");
        requirePositive(event.askPrice(), "askPrice");
        if (event.askPrice().compareTo(event.bidPrice()) < 0) {
            throw new IllegalArgumentException("askPrice must be greater than or equal to bidPrice");
        }
    }

    private void validateMarketPrice(MarketPriceEvent event) {
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (event.symbol() == null || event.symbol().isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        requirePositive(event.price(), "price");
    }

    private void validateTrade(TradeExecutedEvent event) {
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (event.symbol() == null || event.symbol().isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (event.quantity() == null || event.quantity() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        requirePositive(event.price(), "price");
    }

    private void requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }
}
