package com.marketx.pnl.kafka;

import com.marketx.common.events.KafkaTopics;
import com.marketx.common.events.MarketPriceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MarketPricePublisher {
    private static final Logger log = LoggerFactory.getLogger(MarketPricePublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MarketPricePublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(MarketPriceEvent event) {
        kafkaTemplate.send(KafkaTopics.MARKET_PRICES, event.symbol(), event);
        log.info("Published {} eventId={} symbol={}", KafkaTopics.MARKET_PRICES, event.eventId(), event.symbol());
    }
}
