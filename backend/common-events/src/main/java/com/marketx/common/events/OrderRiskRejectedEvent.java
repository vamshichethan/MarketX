package com.marketx.common.events;

import java.time.LocalDateTime;
import java.util.List;

public record OrderRiskRejectedEvent(
        String eventId,
        String orderId,
        String accountId,
        String symbol,
        List<String> reasons,
        LocalDateTime rejectedAt
) {
}
