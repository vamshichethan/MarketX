package com.marketx.marketdata.service;

import com.marketx.marketdata.config.MarketDataConfig;
import com.marketx.marketdata.event.MarketDataEvent;
import com.marketx.marketdata.metrics.MarketDataMetricsService;
import com.marketx.marketdata.producer.MarketDataProducer;
import io.micrometer.core.instrument.Timer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MarketDataGeneratorService {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal BASIS_POINT_DIVISOR = BigDecimal.valueOf(10_000);
    private static final BigDecimal BID_ASK_OFFSET = new BigDecimal("0.05");
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");

    private final AtomicLong eventSequence = new AtomicLong(1);
    private final MarketDataStateService stateService;
    private final MarketDataProducer producer;
    private final MarketDataMetricsService metricsService;
    private volatile boolean publishingEnabled;

    public MarketDataGeneratorService(
            MarketDataConfig config,
            MarketDataStateService stateService,
            MarketDataProducer producer,
            MarketDataMetricsService metricsService
    ) {
        this.publishingEnabled = config.isEnabled();
        this.stateService = stateService;
        this.producer = producer;
        this.metricsService = metricsService;
        this.metricsService.registerPublishingEnabledGauge(this, service -> service.isPublishingEnabled() ? 1 : 0);
    }

    @Scheduled(fixedRateString = "${marketx.market-data.publish-rate-ms:1000}")
    public void publishScheduledTicks() {
        if (!publishingEnabled) {
            return;
        }

        publishOneTickForAllSymbols();
    }

    public List<MarketDataEvent> publishOneTickForAllSymbols() {
        return stateService.getSymbols().stream()
                .map(this::generateAndPublishTick)
                .toList();
    }

    public void startPublishing() {
        publishingEnabled = true;
    }

    public void stopPublishing() {
        publishingEnabled = false;
    }

    public boolean isPublishingEnabled() {
        return publishingEnabled;
    }

    private MarketDataEvent generateAndPublishTick(String symbol) {
        Timer.Sample sample = metricsService.startPublish();
        try {
            BigDecimal previousPrice = stateService.getCurrentPrice(symbol);
            BigDecimal price = nextPrice(previousPrice);
            BigDecimal change = price.subtract(previousPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal changePercent = change
                    .divide(previousPrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal bidPrice = price.subtract(BID_ASK_OFFSET).max(MIN_PRICE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal askPrice = price.add(BID_ASK_OFFSET).setScale(2, RoundingMode.HALF_UP);
            BigDecimal spread = askPrice.subtract(bidPrice).setScale(2, RoundingMode.HALF_UP);

            MarketDataEvent event = new MarketDataEvent(
                    nextEventId(),
                    symbol,
                    price,
                    previousPrice,
                    change,
                    changePercent,
                    nextVolume(),
                    bidPrice,
                    askPrice,
                    spread,
                    LocalDateTime.now()
            );

            stateService.updatePrice(symbol, price);
            stateService.saveLatest(event);
            producer.publish(event);
            metricsService.recordPublishedTick(sample);
            return event;
        } catch (RuntimeException exception) {
            metricsService.recordPublishLatency(sample);
            throw exception;
        }
    }

    private BigDecimal nextPrice(BigDecimal previousPrice) {
        int basisPointMove = ThreadLocalRandom.current().nextInt(-50, 51);
        BigDecimal multiplier = ONE.add(BigDecimal.valueOf(basisPointMove).divide(BASIS_POINT_DIVISOR, 6, RoundingMode.HALF_UP));
        return previousPrice.multiply(multiplier)
                .max(MIN_PRICE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private long nextVolume() {
        return ThreadLocalRandom.current().nextLong(100, 10_001);
    }

    private String nextEventId() {
        return "EVT-" + eventSequence.getAndIncrement();
    }
}
