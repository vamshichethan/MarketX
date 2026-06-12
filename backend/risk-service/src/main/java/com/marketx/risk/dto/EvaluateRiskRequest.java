package com.marketx.risk.dto;

import com.marketx.risk.enums.OrderSide;
import com.marketx.risk.enums.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EvaluateRiskRequest(
        @NotBlank String accountId,
        @NotBlank String symbol,
        @NotNull OrderSide side,
        @NotNull OrderType type,
        int quantity,
        BigDecimal price
) {
}
