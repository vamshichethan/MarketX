package com.marketx.risk.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PnlResponse(
        String accountId,
        String symbol,
        int netQuantity,
        BigDecimal averagePrice,
        BigDecimal realizedPnl,
        BigDecimal unrealizedPnl,
        BigDecimal totalPnl,
        BigDecimal lastMarketPrice,
        String positionType,
        LocalDateTime updatedAt
) {
}
