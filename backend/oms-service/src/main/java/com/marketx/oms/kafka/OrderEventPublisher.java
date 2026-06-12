package com.marketx.oms.kafka;

import com.marketx.common.events.KafkaTopics;
import com.marketx.common.events.OrderSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderSubmitted(OrderSubmittedEvent event) {
        kafkaTemplate.send(KafkaTopics.ORDERS_SUBMITTED, event.orderId(), event);
        log.info("Published {} eventId={} orderId={}", KafkaTopics.ORDERS_SUBMITTED, event.eventId(), event.orderId());
    }
}
