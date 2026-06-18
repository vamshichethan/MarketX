package com.marketx.marketdata.controller;

import com.marketx.marketdata.dto.AddSymbolRequest;
import com.marketx.marketdata.dto.MarketDataResponse;
import com.marketx.marketdata.event.MarketDataEvent;
import com.marketx.marketdata.service.MarketDataGeneratorService;
import com.marketx.marketdata.service.MarketDataStateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/market-data")
public class MarketDataController {
    private final MarketDataStateService stateService;
    private final MarketDataGeneratorService generatorService;

    public MarketDataController(
            MarketDataStateService stateService,
            MarketDataGeneratorService generatorService
    ) {
        this.stateService = stateService;
        this.generatorService = generatorService;
    }

    @GetMapping("/latest")
    public List<MarketDataResponse> latest() {
        return stateService.getLatestEvents().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/latest/{symbol}")
    public ResponseEntity<MarketDataResponse> latestForSymbol(@PathVariable String symbol) {
        return stateService.getLatestEvent(symbol)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/start")
    public Map<String, Object> start() {
        generatorService.startPublishing();
        return Map.of("publishingEnabled", generatorService.isPublishingEnabled());
    }

    @PostMapping("/stop")
    public Map<String, Object> stop() {
        generatorService.stopPublishing();
        return Map.of("publishingEnabled", generatorService.isPublishingEnabled());
    }

    @PostMapping("/tick")
    public List<MarketDataResponse> tick() {
        return generatorService.publishOneTickForAllSymbols().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/symbols")
    public MarketDataResponse addSymbol(@Valid @RequestBody AddSymbolRequest request) {
        return toResponse(stateService.addSymbol(request.symbol(), request.startingPrice()));
    }

    private MarketDataResponse toResponse(MarketDataEvent event) {
        return new MarketDataResponse(
                event.eventId(),
                event.symbol(),
                event.price(),
                event.previousPrice(),
                event.change(),
                event.changePercent(),
                event.volume(),
                event.bidPrice(),
                event.askPrice(),
                event.spread(),
                event.timestamp()
        );
    }
}
