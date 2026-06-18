package com.marketx.analytics.dto;

import java.math.BigDecimal;

public record SymbolDashboardRow(
        String symbol,
        BigDecimal vwap,
        Long volume,
        BigDecimal spread,
        Long tradeCount
) {
}
