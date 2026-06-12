package com.marketx.pnl.dto;

import com.marketx.pnl.enums.OrderSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeEventRequest(
        @NotBlank String tradeId,
        @NotBlank String accountId,
        @NotBlank String symbol,
        @NotNull OrderSide side,
        int quantity,
        BigDecimal price,
        @NotNull LocalDateTime executedAt
) {
}
