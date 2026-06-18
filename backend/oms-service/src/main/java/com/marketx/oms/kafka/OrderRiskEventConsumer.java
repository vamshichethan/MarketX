package com.marketx.oms.kafka;

import com.marketx.common.events.KafkaTopics;
import com.marketx.common.events.OrderCancelRequestedEvent;
import com.marketx.common.events.OrderRiskApprovedEvent;
import com.marketx.common.events.OrderRiskRejectedEvent;
import com.marketx.common.events.TradeExecutedEvent;
import com.marketx.common.events.OrderSubmittedEvent;
import com.marketx.oms.entity.ProcessedEventEntity;
import com.marketx.oms.repository.ProcessedEventRepository;
import com.marketx.oms.service.OrderService;
import com.marketx.oms.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderRiskEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderRiskEventConsumer.class);

    private final OrderService orderService;
    private final TradeService tradeService;
    private final ProcessedEventRepository processedEventRepository;

    public OrderRiskEventConsumer(
            OrderService orderService,
            TradeService tradeService,
            ProcessedEventRepository processedEventRepository
    ) {
        this.orderService = orderService;
        this.tradeService = tradeService;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = KafkaTopics.ORDERS_SUBMITTED)
    public void onOrderSubmitted(OrderSubmittedEvent event) {
        if (skipProcessed(event.eventId(), "OrderSubmittedEvent")) {
            return;
        }

        try {
            log.info("Consumed {} eventId={} orderId={}", KafkaTopics.ORDERS_SUBMITTED, event.eventId(), event.orderId());
            orderService.handleOrderSubmitted(event);
            markProcessed(event.eventId(), "OrderSubmittedEvent");
        } catch (RuntimeException exception) {
            log.error("Failed to process order submitted eventId={} orderId={}", event.eventId(), event.orderId(), exception);
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDERS_RISK_APPROVED)
    public void onRiskApproved(OrderRiskApprovedEvent event) {
        if (skipProcessed(event.eventId(), "OrderRiskApprovedEvent")) {
            return;
        }

        try {
            log.info("Consumed {} eventId={} orderId={}", KafkaTopics.ORDERS_RISK_APPROVED, event.eventId(), event.orderId());
            orderService.handleRiskApproved(event);
            markProcessed(event.eventId(), "OrderRiskApprovedEvent");
        } catch (RuntimeException exception) {
            log.error("Failed to process risk approved eventId={} orderId={}", event.eventId(), event.orderId(), exception);
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDERS_RISK_REJECTED)
    public void onRiskRejected(OrderRiskRejectedEvent event) {
        if (skipProcessed(event.eventId(), "OrderRiskRejectedEvent")) {
            return;
        }

        try {
            log.info("Consumed {} eventId={} orderId={}", KafkaTopics.ORDERS_RISK_REJECTED, event.eventId(), event.orderId());
            orderService.handleRiskRejected(event);
            markProcessed(event.eventId(), "OrderRiskRejectedEvent");
        } catch (RuntimeException exception) {
            log.error("Failed to process risk rejected eventId={} orderId={}", event.eventId(), event.orderId(), exception);
        }
    }

    @KafkaListener(topics = KafkaTopics.TRADES_EXECUTED)
    public void onTradeExecuted(TradeExecutedEvent event) {
        if (skipProcessed(event.eventId(), "TradeExecutedEvent")) {
            return;
        }

        try {
            log.info("Consumed {} eventId={} tradeId={}", KafkaTopics.TRADES_EXECUTED, event.eventId(), event.tradeId());
            tradeService.storeTradeEvent(event);
            markProcessed(event.eventId(), "TradeExecutedEvent");
        } catch (RuntimeException exception) {
            log.error("Failed to process trade executed eventId={} tradeId={}", event.eventId(), event.tradeId(), exception);
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDERS_CANCEL_REQUESTED)
    public void onCancelRequested(OrderCancelRequestedEvent event) {
        if (skipProcessed(event.eventId(), "OrderCancelRequestedEvent")) {
            return;
        }

        try {
            log.info("Consumed {} eventId={} originalOrderId={}", KafkaTopics.ORDERS_CANCEL_REQUESTED, event.eventId(), event.originalOrderId());
            orderService.handleCancelRequested(event);
            markProcessed(event.eventId(), "OrderCancelRequestedEvent");
        } catch (RuntimeException exception) {
            log.error("Failed to process cancel requested eventId={} originalOrderId={}", event.eventId(), event.originalOrderId(), exception);
        }
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
}
