package com.marketx.risk.dto;

import java.math.BigDecimal;

public record RiskLimitRequest(
        int maxOrderQuantity,
        int maxPositionQuantity,
        BigDecimal maxExposure,
        BigDecimal maxDailyLoss
) {
}
