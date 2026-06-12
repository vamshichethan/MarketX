package com.marketx.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
    private final long orderId;
    private final OrderSide side;
    private final OrderType orderType;
    private final String symbol;
    private int quantity;
    private final BigDecimal price;
    private final LocalDateTime timestamp;

    public Order(
            long orderId,
            OrderSide side,
            OrderType orderType,
            String symbol,
            int quantity,
            BigDecimal price,
            LocalDateTime timestamp
    ) {
        this.orderId = orderId;
        this.side = side;
        this.orderType = orderType;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
    }

    public long getOrderId() {
        return orderId;
    }

    public OrderSide getSide() {
        return side;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void reduceQuantity(int executedQuantity) {
        if (executedQuantity < 0 || executedQuantity > quantity) {
            throw new IllegalArgumentException("Executed quantity must be between 0 and remaining order quantity.");
        }

        quantity -= executedQuantity;
    }

    public boolean isFilled() {
        return quantity == 0;
    }
}
