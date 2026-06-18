package com.marketx.replay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketDataEvent(
        String eventId,
        String symbol,
        BigDecimal price,
        BigDecimal previousPrice,
        BigDecimal change,
        BigDecimal changePercent,
        long volume,
        BigDecimal bidPrice,
        BigDecimal askPrice,
        BigDecimal spread,
        LocalDateTime timestamp,
        String source,
        String replaySessionId
) {
}
