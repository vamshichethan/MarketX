package com.marketx.oms.dto;

import java.math.BigDecimal;

public record OrderBookLevelResponse(
        BigDecimal price,
        int quantity
) {
}
