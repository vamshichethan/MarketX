package com.marketx.oms.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderBookResponse(
        String symbol,
        List<OrderBookLevelResponse> bids,
        List<OrderBookLevelResponse> asks,
        BigDecimal bestBid,
        BigDecimal bestAsk,
        BigDecimal spread
) {
}
