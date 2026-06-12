package com.marketx.oms.dto;

import com.marketx.oms.enums.OrderSide;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeResponse(
        String tradeId,
        String symbol,
        int quantity,
        BigDecimal price,
        String buyOrderId,
        String sellOrderId,
        OrderSide aggressorSide,
        LocalDateTime executedAt
) {
}
