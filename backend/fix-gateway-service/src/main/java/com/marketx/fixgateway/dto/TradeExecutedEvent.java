package com.marketx.fixgateway.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeExecutedEvent(
        String eventId,
        String tradeId,
        String symbol,
        Integer quantity,
        BigDecimal price,
        String buyOrderId,
        String sellOrderId,
        String buyAccountId,
        String sellAccountId,
        String aggressorSide,
        LocalDateTime executedAt
) {
}
