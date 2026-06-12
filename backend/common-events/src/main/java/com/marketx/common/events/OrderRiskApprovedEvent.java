package com.marketx.common.events;

import java.time.LocalDateTime;

public record OrderRiskApprovedEvent(
        String eventId,
        String orderId,
        String accountId,
        String symbol,
        LocalDateTime approvedAt,
        String message
) {
}
