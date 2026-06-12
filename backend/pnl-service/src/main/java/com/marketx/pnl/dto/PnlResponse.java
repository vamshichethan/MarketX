package com.marketx.pnl.dto;

import com.marketx.pnl.enums.PositionType;

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
        PositionType positionType,
        LocalDateTime updatedAt
) {
}
