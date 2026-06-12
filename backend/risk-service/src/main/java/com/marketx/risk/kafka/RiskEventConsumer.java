package com.marketx.risk.kafka;

import com.marketx.common.events.KafkaTopics;
import com.marketx.common.events.MarketPriceEvent;
import com.marketx.common.events.OrderRiskApprovedEvent;
import com.marketx.common.events.OrderRiskRejectedEvent;
import com.marketx.common.events.OrderSubmittedEvent;
import com.marketx.risk.dto.EvaluateRiskRequest;
import com.marketx.risk.dto.MarketPriceRequest;
import com.marketx.risk.dto.RiskEvaluationResponse;
import com.marketx.risk.entity.ProcessedEventEntity;
import com.marketx.risk.enums.OrderSide;
import com.marketx.risk.enums.OrderType;
import com.marketx.risk.repository.ProcessedEventRepository;
import com.marketx.risk.service.RiskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class RiskEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(RiskEventConsumer.class);

    private final RiskService riskService;
    private final RiskEventPublisher riskEventPublisher;
    private final ProcessedEventRepository processedEventRepository;

    public RiskEventConsumer(
            RiskService riskService,
            RiskEventPublisher riskEventPublisher,
            ProcessedEventRepository processedEventRepository
    ) {
        this.riskService = riskService;
        this.riskEventPublisher = riskEventPublisher;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = KafkaTopics.ORDERS_SUBMITTED)
    public void onOrderSubmitted(OrderSubmittedEvent event) {
        if (skipProcessed(event.eventId(), "OrderSubmittedEvent")) {
            return;
        }

        try {
            log.info("Consumed {} eventId={} orderId={}", KafkaTopics.ORDERS_SUBMITTED, event.eventId(), event.orderId());
            RiskEvaluationResponse response = riskService.evaluate(new EvaluateRiskRequest(
                    event.accountId(),
                    event.symbol(),
                    OrderSide.valueOf(event.side()),
                    OrderType.valueOf(event.type()),
                    event.quantity(),
                    event.price()
            ));

            if (response.approved()) {
                riskEventPublisher.publishApproved(new OrderRiskApprovedEvent(
                        UUID.randomUUID().toString(),
                        event.orderId(),
                        event.accountId(),
                        event.symbol(),
                        LocalDateTime.now(),
                        "Order approved by Risk Service"
                ));
            } else {
                riskEventPublisher.publishRejected(new OrderRiskRejectedEvent(
                        UUID.randomUUID().toString(),
                        event.orderId(),
                        event.accountId(),
                        event.symbol(),
                        response.reasons(),
                        LocalDateTime.now()
                ));
            }

            markProcessed(event.eventId(), "OrderSubmittedEvent");
        } catch (RuntimeException exception) {
            log.error("Failed to process order submitted eventId={} orderId={}", event.eventId(), event.orderId(), exception);
        }
    }

    @KafkaListener(topics = KafkaTopics.MARKET_PRICES)
    public void onMarketPrice(MarketPriceEvent event) {
        if (skipProcessed(event.eventId(), "MarketPriceEvent")) {
            return;
        }

        try {
            log.info("Consumed {} eventId={} symbol={}", KafkaTopics.MARKET_PRICES, event.eventId(), event.symbol());
            riskService.updateMarketPrice(new MarketPriceRequest(event.symbol(), event.price(), event.timestamp()));
            markProcessed(event.eventId(), "MarketPriceEvent");
        } catch (RuntimeException exception) {
            log.error("Failed to process market price eventId={} symbol={}", event.eventId(), event.symbol(), exception);
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
