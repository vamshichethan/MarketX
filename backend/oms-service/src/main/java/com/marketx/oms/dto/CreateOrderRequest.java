package com.marketx.oms.dto;

import com.marketx.oms.enums.OrderSide;
import com.marketx.oms.enums.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderRequest(
        String accountId,
        @NotBlank String symbol,
        @NotNull OrderSide side,
        @NotNull OrderType type,
        int quantity,
        BigDecimal price
) {
}
