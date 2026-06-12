package com.marketx.oms.dto;

import java.math.BigDecimal;

public record ModifyOrderRequest(
        int quantity,
        BigDecimal price
) {
}
