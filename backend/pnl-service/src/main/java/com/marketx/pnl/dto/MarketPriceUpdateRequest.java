package com.marketx.pnl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketPriceUpdateRequest(
        @NotBlank String symbol,
        BigDecimal price,
        @NotNull LocalDateTime timestamp
) {
}
