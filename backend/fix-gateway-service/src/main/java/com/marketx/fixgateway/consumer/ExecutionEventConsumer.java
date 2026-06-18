package com.marketx.fixgateway.consumer;

import com.marketx.fixgateway.dto.OrderCancelledEvent;
import com.marketx.fixgateway.dto.OrderRiskRejectedEvent;
import com.marketx.fixgateway.dto.TradeExecutedEvent;
import com.marketx.fixgateway.service.FixGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ExecutionEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(ExecutionEventConsumer.class);

    private final FixGatewayService fixGatewayService;

    public ExecutionEventConsumer(FixGatewayService fixGatewayService) {
        this.fixGatewayService = fixGatewayService;
    }

    @KafkaListener(topics = "trades.executed", groupId = "fix-gateway-service-group")
    public void onTradeExecuted(TradeExecutedEvent event) {
        try {
            log.info("Consumed trades.executed eventId={} tradeId={}", event.eventId(), event.tradeId());
            fixGatewayService.processTradeExecuted(event);
        } catch (RuntimeException exception) {
            log.error("Failed to convert trade to FIX execution report eventId={} tradeId={}", event.eventId(), event.tradeId(), exception);
        }
    }

    @KafkaListener(topics = "orders.risk.rejected", groupId = "fix-gateway-service-group")
    public void onRiskRejected(OrderRiskRejectedEvent event) {
        try {
            log.info("Consumed orders.risk.rejected eventId={} orderId={}", event.eventId(), event.orderId());
            fixGatewayService.processRiskRejected(event);
        } catch (RuntimeException exception) {
            log.error("Failed to convert rejection to FIX execution report eventId={} orderId={}", event.eventId(), event.orderId(), exception);
        }
    }

    @KafkaListener(topics = "orders.cancelled", groupId = "fix-gateway-service-group")
    public void onOrderCancelled(OrderCancelledEvent event) {
        try {
            log.info("Consumed orders.cancelled eventId={} orderId={}", event.eventId(), event.orderId());
            fixGatewayService.processOrderCancelled(event);
        } catch (RuntimeException exception) {
            log.error("Failed to convert cancellation to FIX execution report eventId={} orderId={}", event.eventId(), event.orderId(), exception);
        }
    }
}
