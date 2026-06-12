package com.marketx.oms.dto;

import com.marketx.oms.enums.OrderSide;
import com.marketx.oms.enums.OrderStatus;
import com.marketx.oms.enums.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        String orderId,
        String accountId,
        String symbol,
        OrderSide side,
        OrderType type,
        int originalQuantity,
        int remainingQuantity,
        BigDecimal price,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> rejectionReasons
) {
}
