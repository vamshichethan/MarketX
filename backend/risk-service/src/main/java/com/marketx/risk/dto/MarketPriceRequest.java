package com.marketx.risk.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketPriceRequest(
        @NotBlank String symbol,
        BigDecimal price,
        LocalDateTime timestamp
) {
}
