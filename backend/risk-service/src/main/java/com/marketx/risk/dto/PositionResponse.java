package com.marketx.risk.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PositionResponse(
        String accountId,
        String symbol,
        int netQuantity,
        BigDecimal averagePrice,
        String positionType,
        LocalDateTime updatedAt
) {
}
