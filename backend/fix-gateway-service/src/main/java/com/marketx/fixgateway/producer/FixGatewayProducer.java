package com.marketx.fixgateway.producer;

import com.marketx.common.events.KafkaTopics;
import com.marketx.common.events.OrderCancelRequestedEvent;
import com.marketx.common.events.OrderSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FixGatewayProducer {
    private static final Logger log = LoggerFactory.getLogger(FixGatewayProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FixGatewayProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishInboundFix(String clOrdId, String rawMessage) {
        kafkaTemplate.send(KafkaTopics.FIX_INBOUND, clOrdId, rawMessage);
        log.info("Published {} clOrdId={}", KafkaTopics.FIX_INBOUND, clOrdId);
    }

    public void publishOrderSubmitted(OrderSubmittedEvent event) {
        kafkaTemplate.send(KafkaTopics.ORDERS_SUBMITTED, event.orderId(), event);
        log.info("Published {} eventId={} orderId={}", KafkaTopics.ORDERS_SUBMITTED, event.eventId(), event.orderId());
    }

    public void publishCancelRequested(OrderCancelRequestedEvent event) {
        kafkaTemplate.send(KafkaTopics.ORDERS_CANCEL_REQUESTED, event.originalOrderId(), event);
        log.info("Published {} eventId={} originalOrderId={}", KafkaTopics.ORDERS_CANCEL_REQUESTED, event.eventId(), event.originalOrderId());
    }

    public void publishExecutionReport(String clOrdId, String rawFixMessage) {
        kafkaTemplate.send(KafkaTopics.FIX_EXECUTION_REPORTS, clOrdId, rawFixMessage);
        log.info("Published {} clOrdId={}", KafkaTopics.FIX_EXECUTION_REPORTS, clOrdId);
    }
}
