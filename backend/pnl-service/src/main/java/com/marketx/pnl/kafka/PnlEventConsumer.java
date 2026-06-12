package com.marketx.pnl.kafka;

import com.marketx.common.events.KafkaTopics;
import com.marketx.common.events.MarketPriceEvent;
import com.marketx.common.events.TradeExecutedEvent;
import com.marketx.pnl.dto.MarketPriceUpdateRequest;
import com.marketx.pnl.dto.TradeEventRequest;
import com.marketx.pnl.entity.ProcessedEventEntity;
import com.marketx.pnl.enums.OrderSide;
import com.marketx.pnl.exception.DuplicateTradeException;
import com.marketx.pnl.repository.ProcessedEventRepository;
import com.marketx.pnl.service.PnlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PnlEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(PnlEventConsumer.class);

    private final PnlService pnlService;
    private final ProcessedEventRepository processedEventRepository;

    public PnlEventConsumer(PnlService pnlService, ProcessedEventRepository processedEventRepository) {
        this.pnlService = pnlService;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = KafkaTopics.TRADES_EXECUTED)
    public void onTradeExecuted(TradeExecutedEvent event) {
        if (skipProcessed(event.eventId(), "TradeExecutedEvent")) {
            return;
        }

        try {
            log.info("Consumed {} eventId={} tradeId={}", KafkaTopics.TRADES_EXECUTED, event.eventId(), event.tradeId());
            processSide(event.tradeId() + "-BUY", event.buyAccountId(), event, OrderSide.BUY);
            processSide(event.tradeId() + "-SELL", event.sellAccountId(), event, OrderSide.SELL);
            markProcessed(event.eventId(), "TradeExecutedEvent");
        } catch (RuntimeException exception) {
            log.error("Failed to process trade executed eventId={} tradeId={}", event.eventId(), event.tradeId(), exception);
        }
    }

    @KafkaListener(topics = KafkaTopics.MARKET_PRICES)
    public void onMarketPrice(MarketPriceEvent event) {
        if (skipProcessed(event.eventId(), "MarketPriceEvent")) {
            return;
        }

        try {
            log.info("Consumed {} eventId={} symbol={}", KafkaTopics.MARKET_PRICES, event.eventId(), event.symbol());
            pnlService.updateMarketPrice(new MarketPriceUpdateRequest(event.symbol(), event.price(), event.timestamp()));
            markProcessed(event.eventId(), "MarketPriceEvent");
        } catch (RuntimeException exception) {
            log.error("Failed to process market price eventId={} symbol={}", event.eventId(), event.symbol(), exception);
        }
    }

    private void processSide(String tradeId, String accountId, TradeExecutedEvent event, OrderSide side) {
        try {
            pnlService.processTrade(new TradeEventRequest(
                    tradeId,
                    accountId,
                    event.symbol(),
                    side,
                    event.quantity(),
                    event.price(),
                    event.executedAt()
            ));
        } catch (DuplicateTradeException exception) {
            log.info("Skipping duplicate PnL trade event tradeId={}", tradeId);
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
