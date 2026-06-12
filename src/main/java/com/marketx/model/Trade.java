package com.marketx.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Trade {
    private final long tradeId;
    private final long buyOrderId;
    private final long sellOrderId;
    private final String symbol;
    private final int quantity;
    private final BigDecimal price;
    private final LocalDateTime timestamp;

    public Trade(
            long tradeId,
            long buyOrderId,
            long sellOrderId,
            String symbol,
            int quantity,
            BigDecimal price,
            LocalDateTime timestamp
    ) {
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
    }

    public long getTradeId() {
        return tradeId;
    }

    public long getBuyOrderId() {
        return buyOrderId;
    }

    public long getSellOrderId() {
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
