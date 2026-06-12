package com.marketx.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Trade {
    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final String symbol;
    private final int quantity;
    private final BigDecimal price;
    private final OrderSide aggressorSide;
    private final LocalDateTime timestamp;

    public Trade(
            String tradeId,
            String buyOrderId,
            String sellOrderId,
            String symbol,
            int quantity,
            BigDecimal price,
            OrderSide aggressorSide,
            LocalDateTime timestamp
    ) {
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.aggressorSide = aggressorSide;
        this.timestamp = timestamp;
    }

    public String getTradeId() {
        return tradeId;
    }

    public String getBuyOrderId() {
        return buyOrderId;
    }

    public String getSellOrderId() {
        return sellOrderId;
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

    public OrderSide getAggressorSide() {
        return aggressorSide;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
