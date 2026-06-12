package com.marketx.engine;

import java.math.BigDecimal;

public class OrderBookLevel {
    private final BigDecimal price;
    private final int totalQuantity;

    public OrderBookLevel(BigDecimal price, int totalQuantity) {
        this.price = price;
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }
}
