package com.marketx.risk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_limits")
public class RiskLimitEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountId;

    private int maxOrderQuantity;
    private int maxPositionQuantity;
    private BigDecimal maxExposure;
    private BigDecimal maxDailyLoss;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public int getMaxOrderQuantity() {
        return maxOrderQuantity;
    }

    public void setMaxOrderQuantity(int maxOrderQuantity) {
        this.maxOrderQuantity = maxOrderQuantity;
    }

    public int getMaxPositionQuantity() {
        return maxPositionQuantity;
    }

    public void setMaxPositionQuantity(int maxPositionQuantity) {
        this.maxPositionQuantity = maxPositionQuantity;
    }

    public BigDecimal getMaxExposure() {
        return maxExposure;
    }

    public void setMaxExposure(BigDecimal maxExposure) {
        this.maxExposure = maxExposure;
    }

    public BigDecimal getMaxDailyLoss() {
        return maxDailyLoss;
    }

    public void setMaxDailyLoss(BigDecimal maxDailyLoss) {
        this.maxDailyLoss = maxDailyLoss;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
