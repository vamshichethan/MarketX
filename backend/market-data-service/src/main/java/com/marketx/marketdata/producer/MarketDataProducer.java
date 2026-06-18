package com.marketx.marketdata.producer;

import com.marketx.common.events.KafkaTopics;
import com.marketx.common.events.MarketPriceEvent;
import com.marketx.marketdata.event.MarketDataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MarketDataProducer {
    private static final Logger log = LoggerFactory.getLogger(MarketDataProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MarketDataProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(MarketDataEvent event) {
        kafkaTemplate.send(KafkaTopics.MARKET_DATA, event.symbol(), event);
        kafkaTemplate.send(KafkaTopics.MARKET_PRICES, event.symbol(), toMarketPriceEvent(event));

        log.info(
                "Published market data: {} price={} bid={} ask={} volume={}",
                event.symbol(),
                event.price(),
                event.bidPrice(),
                event.askPrice(),
                event.volume()
        );
    }

    private MarketPriceEvent toMarketPriceEvent(MarketDataEvent event) {
        return new MarketPriceEvent(event.eventId(), event.symbol(), event.price(), event.timestamp());
    }
}
