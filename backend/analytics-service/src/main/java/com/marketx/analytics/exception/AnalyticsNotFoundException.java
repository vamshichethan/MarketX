package com.marketx.analytics.exception;

public class AnalyticsNotFoundException extends RuntimeException {
    public AnalyticsNotFoundException(String symbol) {
        super("Analytics not found for symbol: " + symbol);
    }
}
