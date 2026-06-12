package com.marketx.position.kafka;

import com.marketx.common.events.KafkaTopics;
import com.marketx.common.events.TradeExecutedEvent;
import com.marketx.position.dto.TradeEventRequest;
import com.marketx.position.entity.ProcessedEventEntity;
import com.marketx.position.enums.OrderSide;
import com.marketx.position.exception.DuplicateTradeException;
import com.marketx.position.repository.ProcessedEventRepository;
import com.marketx.position.service.PositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TradeExecutedConsumer {
    private static final Logger log = LoggerFactory.getLogger(TradeExecutedConsumer.class);

    private final PositionService positionService;
    private final ProcessedEventRepository processedEventRepository;

    public TradeExecutedConsumer(PositionService positionService, ProcessedEventRepository processedEventRepository) {
        this.positionService = positionService;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = KafkaTopics.TRADES_EXECUTED)
    public void onTradeExecuted(TradeExecutedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("Skipping duplicate TradeExecutedEvent eventId={}", event.eventId());
            return;
        }

        try {
            log.info("Consumed {} eventId={} tradeId={}", KafkaTopics.TRADES_EXECUTED, event.eventId(), event.tradeId());
            processSide(event.tradeId() + "-BUY", event.buyAccountId(), event, OrderSide.BUY);
            processSide(event.tradeId() + "-SELL", event.sellAccountId(), event, OrderSide.SELL);
            markProcessed(event.eventId());
        } catch (RuntimeException exception) {
            log.error("Failed to process trade executed eventId={} tradeId={}", event.eventId(), event.tradeId(), exception);
        }
    }

    private void processSide(String tradeId, String accountId, TradeExecutedEvent event, OrderSide side) {
        try {
            positionService.processTrade(new TradeEventRequest(
                    tradeId,
                    accountId,
                    event.symbol(),
                    side,
                    event.quantity(),
                    event.price(),
                    event.executedAt()
            ));
        } catch (DuplicateTradeException exception) {
            log.info("Skipping duplicate position trade event tradeId={}", tradeId);
        }
    }

    private void markProcessed(String eventId) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(eventId);
        entity.setEventType("TradeExecutedEvent");
        entity.setProcessedAt(LocalDateTime.now());
        processedEventRepository.save(entity);
    }
}
