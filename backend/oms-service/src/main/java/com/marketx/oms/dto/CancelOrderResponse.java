package com.marketx.oms.dto;

import com.marketx.oms.enums.OrderStatus;

public record CancelOrderResponse(
        String orderId,
        OrderStatus status,
        String message
) {
}
