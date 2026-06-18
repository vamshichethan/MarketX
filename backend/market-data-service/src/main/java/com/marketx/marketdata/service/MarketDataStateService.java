package com.marketx.marketdata.service;

import com.marketx.marketdata.config.MarketDataConfig;
import com.marketx.marketdata.event.MarketDataEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketDataStateService {
    private final Map<String, BigDecimal> currentPrices = new ConcurrentHashMap<>();
    private final Map<String, MarketDataEvent> latestEvents = new ConcurrentHashMap<>();

    public MarketDataStateService(MarketDataConfig config) {
        config.getSymbols().forEach((symbol, price) -> addSymbol(symbol, price));
    }

    public List<String> getSymbols() {
        return currentPrices.keySet().stream()
                .sorted()
                .toList();
    }

    public BigDecimal getCurrentPrice(String symbol) {
        return currentPrices.get(normalizeSymbol(symbol));
    }

    public void updatePrice(String symbol, BigDecimal price) {
        currentPrices.put(normalizeSymbol(symbol), price);
    }

    public void saveLatest(MarketDataEvent event) {
        latestEvents.put(event.symbol(), event);
    }

    public List<MarketDataEvent> getLatestEvents() {
        return latestEvents.values().stream()
                .sorted(Comparator.comparing(MarketDataEvent::symbol))
                .toList();
    }

    public Optional<MarketDataEvent> getLatestEvent(String symbol) {
        return Optional.ofNullable(latestEvents.get(normalizeSymbol(symbol)));
    }

    public MarketDataEvent addSymbol(String symbol, BigDecimal startingPrice) {
        String normalizedSymbol = normalizeSymbol(symbol);
        currentPrices.put(normalizedSymbol, startingPrice);

        MarketDataEvent snapshot = new MarketDataEvent(
                "EVT-INIT-" + normalizedSymbol,
                normalizedSymbol,
                startingPrice,
                startingPrice,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                startingPrice,
                startingPrice,
                BigDecimal.ZERO,
                LocalDateTime.now()
        );
        saveLatest(snapshot);
        return snapshot;
    }

    private String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }
}
