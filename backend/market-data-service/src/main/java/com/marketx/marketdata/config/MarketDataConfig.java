package com.marketx.marketdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "marketx.market-data")
public class MarketDataConfig {
    private Map<String, BigDecimal> symbols = new LinkedHashMap<>();
    private long publishRateMs = 1000;
    private boolean enabled = true;

    public Map<String, BigDecimal> getSymbols() {
        return symbols;
    }

    public void setSymbols(Map<String, BigDecimal> symbols) {
        this.symbols = symbols;
    }

    public long getPublishRateMs() {
        return publishRateMs;
    }

    public void setPublishRateMs(long publishRateMs) {
        this.publishRateMs = publishRateMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
