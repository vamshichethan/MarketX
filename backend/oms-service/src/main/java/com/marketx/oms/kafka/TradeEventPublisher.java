package com.marketx.oms.kafka;

import com.marketx.common.events.KafkaTopics;
import com.marketx.common.events.TradeExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TradeEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(TradeEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TradeEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTradeExecuted(TradeExecutedEvent event) {
        kafkaTemplate.send(KafkaTopics.TRADES_EXECUTED, event.tradeId(), event);
        log.info("Published {} eventId={} tradeId={}", KafkaTopics.TRADES_EXECUTED, event.eventId(), event.tradeId());
    }
}
