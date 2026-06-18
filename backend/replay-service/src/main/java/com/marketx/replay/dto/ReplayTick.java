package com.marketx.replay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReplayTick(
        LocalDateTime timestamp,
        String symbol,
        BigDecimal price,
        long volume,
        BigDecimal bidPrice,
        BigDecimal askPrice,
        BigDecimal spread
) {
}
