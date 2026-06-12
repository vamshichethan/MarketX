package com.marketx.pnl.controller;

import com.marketx.common.events.MarketPriceEvent;
import com.marketx.pnl.dto.MarketPriceUpdateRequest;
import com.marketx.pnl.kafka.MarketPricePublisher;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/market-prices")
public class MarketPriceController {
    private final MarketPricePublisher marketPricePublisher;

    public MarketPriceController(MarketPricePublisher marketPricePublisher) {
        this.marketPricePublisher = marketPricePublisher;
    }

    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publish(@Valid @RequestBody MarketPriceUpdateRequest request) {
        MarketPriceEvent event = new MarketPriceEvent(
                UUID.randomUUID().toString(),
                request.symbol().toUpperCase(),
                request.price(),
                request.timestamp()
        );
        marketPricePublisher.publish(event);
        return ResponseEntity.ok(Map.of(
                "eventId", event.eventId(),
                "topic", "market.prices",
                "message", "Market price event published"
        ));
    }
}
