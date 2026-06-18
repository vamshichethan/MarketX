package com.marketx.analytics.controller;

import com.marketx.analytics.dto.AnalyticsResponse;
import com.marketx.analytics.dto.DashboardResponse;
import com.marketx.analytics.dto.MarketDataEvent;
import com.marketx.analytics.dto.TradeExecutedEvent;
import com.marketx.analytics.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{symbol}")
    public AnalyticsResponse getSymbolAnalytics(@PathVariable String symbol) {
        return analyticsService.getAnalytics(symbol);
    }

    @GetMapping
    public List<AnalyticsResponse> getAllAnalytics() {
        return analyticsService.getAllAnalytics();
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        return analyticsService.getDashboard();
    }

    @DeleteMapping("/{symbol}/reset")
    public ResponseEntity<Map<String, String>> resetSymbol(@PathVariable String symbol) {
        analyticsService.resetSymbol(symbol);
        return ResponseEntity.ok(Map.of("message", "Analytics reset for symbol: " + symbol.toUpperCase()));
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, String>> resetAll() {
        analyticsService.resetAll();
        return ResponseEntity.ok(Map.of("message", "All analytics reset"));
    }

    @PostMapping("/events/trade")
    public AnalyticsResponse processTrade(@RequestBody TradeExecutedEvent event) {
        return analyticsService.processTrade(event);
    }

    @PostMapping("/events/market-data")
    public AnalyticsResponse processMarketData(@RequestBody MarketDataEvent event) {
        return analyticsService.processMarketData(event);
    }
}
