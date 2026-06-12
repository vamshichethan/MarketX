package com.marketx.risk.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RiskLimitResponse(
        String accountId,
        int maxOrderQuantity,
        int maxPositionQuantity,
        BigDecimal maxExposure,
        BigDecimal maxDailyLoss,
        LocalDateTime updatedAt
) {
}
