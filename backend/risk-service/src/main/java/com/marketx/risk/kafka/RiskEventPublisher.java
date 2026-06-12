package com.marketx.risk.kafka;

import com.marketx.common.events.KafkaTopics;
import com.marketx.common.events.OrderRiskApprovedEvent;
import com.marketx.common.events.OrderRiskRejectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RiskEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(RiskEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RiskEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishApproved(OrderRiskApprovedEvent event) {
        kafkaTemplate.send(KafkaTopics.ORDERS_RISK_APPROVED, event.orderId(), event);
        log.info("Published {} eventId={} orderId={}", KafkaTopics.ORDERS_RISK_APPROVED, event.eventId(), event.orderId());
    }

    public void publishRejected(OrderRiskRejectedEvent event) {
        kafkaTemplate.send(KafkaTopics.ORDERS_RISK_REJECTED, event.orderId(), event);
        log.info("Published {} eventId={} orderId={}", KafkaTopics.ORDERS_RISK_REJECTED, event.eventId(), event.orderId());
    }
}
