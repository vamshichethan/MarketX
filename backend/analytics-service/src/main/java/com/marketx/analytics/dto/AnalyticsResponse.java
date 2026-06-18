package com.marketx.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AnalyticsResponse(
        String symbol,
        BigDecimal vwap,
        Long volume,
        BigDecimal spread,
        Long tradeCount,
        BigDecimal latestPrice,
        BigDecimal bidPrice,
        BigDecimal askPrice,
        LocalDateTime lastUpdatedAt
) {
}
