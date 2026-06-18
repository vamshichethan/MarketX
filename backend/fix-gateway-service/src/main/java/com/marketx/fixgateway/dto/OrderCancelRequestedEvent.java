package com.marketx.fixgateway.dto;

import java.time.LocalDateTime;

public record OrderCancelRequestedEvent(
        String eventId,
        String cancelRequestId,
        String originalOrderId,
        String accountId,
        String symbol,
        String side,
        LocalDateTime requestedAt
) {
}
