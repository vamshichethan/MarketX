package com.marketx.analytics.consumer;

import com.marketx.analytics.dto.MarketDataEvent;
import com.marketx.analytics.dto.MarketPriceEvent;
import com.marketx.analytics.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MarketDataConsumer {
    private static final Logger log = LoggerFactory.getLogger(MarketDataConsumer.class);

    private final AnalyticsService analyticsService;

    public MarketDataConsumer(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @KafkaListener(topics = "market-data", groupId = "analytics-service-group")
    public void onMarketData(MarketDataEvent event) {
        try {
            log.info("Consumed market-data eventId={} symbol={}", event.eventId(), event.symbol());
            analyticsService.processMarketData(event);
        } catch (RuntimeException exception) {
            log.error("Failed to process market-data eventId={} symbol={}", event.eventId(), event.symbol(), exception);
        }
    }

    @KafkaListener(topics = "market.prices", groupId = "analytics-service-group")
    public void onMarketPrice(MarketPriceEvent event) {
        try {
            log.info("Consumed market.prices eventId={} symbol={}", event.eventId(), event.symbol());
            analyticsService.processMarketPrice(event);
        } catch (RuntimeException exception) {
            log.error("Failed to process market.prices eventId={} symbol={}", event.eventId(), event.symbol(), exception);
        }
    }
}
