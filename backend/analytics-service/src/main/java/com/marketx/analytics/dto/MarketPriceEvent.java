package com.marketx.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketPriceEvent(
        String eventId,
        String symbol,
        BigDecimal price,
        LocalDateTime timestamp
) {
}
