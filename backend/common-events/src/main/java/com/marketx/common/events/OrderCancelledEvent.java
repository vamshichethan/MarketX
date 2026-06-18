package com.marketx.common.events;

import java.time.LocalDateTime;

public record OrderCancelledEvent(
        String eventId,
        String orderId,
        String accountId,
        String symbol,
        String side,
        LocalDateTime cancelledAt,
        String message
) {
}
