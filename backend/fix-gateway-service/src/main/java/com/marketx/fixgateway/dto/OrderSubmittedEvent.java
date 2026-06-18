package com.marketx.fixgateway.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSubmittedEvent(
        String eventId,
        String orderId,
        String accountId,
        String symbol,
        String side,
        String type,
        int quantity,
        BigDecimal price,
        LocalDateTime submittedAt
) {
}
