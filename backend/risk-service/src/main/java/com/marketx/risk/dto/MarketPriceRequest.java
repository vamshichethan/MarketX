package com.marketx.risk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketPriceRequest(
        @NotBlank String symbol,
        @NotNull
        BigDecimal price,
        LocalDateTime timestamp
) {
}
