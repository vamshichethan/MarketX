package com.marketx.analytics.consumer;

import com.marketx.analytics.dto.TradeExecutedEvent;
import com.marketx.analytics.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TradeExecutedConsumer {
    private static final Logger log = LoggerFactory.getLogger(TradeExecutedConsumer.class);

    private final AnalyticsService analyticsService;

    public TradeExecutedConsumer(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @KafkaListener(topics = "trades.executed", groupId = "analytics-service-group")
    public void onTradeExecuted(TradeExecutedEvent event) {
        try {
            log.info("Consumed trades.executed eventId={} tradeId={} symbol={}", event.eventId(), event.tradeId(), event.symbol());
            analyticsService.processTrade(event);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to process trades.executed eventId={} tradeId={} symbol={}",
                    event.eventId(),
                    event.tradeId(),
                    event.symbol(),
                    exception
            );
        }
    }
}
