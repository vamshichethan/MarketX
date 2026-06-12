package com.marketx.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExecutionReport {
    private final String executionId;
    private final String orderId;
    private final String symbol;
    private final OrderSide side;
    private final OrderStatus status;
    private final int executedQuantity;
    private final BigDecimal executedPrice;
    private final int remainingQuantity;
    private final String message;
    private final LocalDateTime timestamp;

    public ExecutionReport(
            String executionId,
            String orderId,
            String symbol,
            OrderSide side,
            OrderStatus status,
            int executedQuantity,
            BigDecimal executedPrice,
            int remainingQuantity,
            String message,
            LocalDateTime timestamp
    ) {
        this.executionId = executionId;
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.status = status;
        this.executedQuantity = executedQuantity;
        this.executedPrice = executedPrice;
        this.remainingQuantity = remainingQuantity;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public int getExecutedQuantity() {
        return executedQuantity;
    }

    public BigDecimal getExecutedPrice() {
        return executedPrice;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
