package com.marketx.replay.producer;

import com.marketx.common.events.KafkaTopics;
import com.marketx.common.events.MarketPriceEvent;
import com.marketx.replay.dto.MarketDataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReplayMarketDataProducer {
    private static final Logger log = LoggerFactory.getLogger(ReplayMarketDataProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReplayMarketDataProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(MarketDataEvent event) {
        kafkaTemplate.send(KafkaTopics.MARKET_DATA, event.symbol(), event);
        kafkaTemplate.send(KafkaTopics.MARKET_PRICES, event.symbol(), new MarketPriceEvent(
                event.eventId(),
                event.symbol(),
                event.price(),
                event.timestamp()
        ));
        log.info("Published replay tick eventId={} sessionId={} symbol={}", event.eventId(), event.replaySessionId(), event.symbol());
    }
}
