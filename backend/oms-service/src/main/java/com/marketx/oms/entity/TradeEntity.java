package com.marketx.oms.entity;

import com.marketx.oms.enums.OrderSide;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
public class TradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tradeId;
    private String symbol;
    private int quantity;
    private BigDecimal price;
    private String buyOrderId;
    private String sellOrderId;
    private String buyAccountId;
    private String sellAccountId;

    @Enumerated(EnumType.STRING)
    private OrderSide aggressorSide;

    private LocalDateTime executedAt;

    public Long getId() {
        return id;
    }

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getBuyOrderId() {
        return buyOrderId;
    }

    public void setBuyOrderId(String buyOrderId) {
        this.buyOrderId = buyOrderId;
    }

    public String getSellOrderId() {
        return sellOrderId;
    }

    public void setSellOrderId(String sellOrderId) {
        this.sellOrderId = sellOrderId;
    }

    public String getBuyAccountId() {
        return buyAccountId;
    }

    public void setBuyAccountId(String buyAccountId) {
        this.buyAccountId = buyAccountId;
    }

    public String getSellAccountId() {
        return sellAccountId;
    }

    public void setSellAccountId(String sellAccountId) {
        this.sellAccountId = sellAccountId;
    }

    public OrderSide getAggressorSide() {
        return aggressorSide;
    }

    public void setAggressorSide(OrderSide aggressorSide) {
        this.aggressorSide = aggressorSide;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
