package com.marketx.risk.exception;

public class RiskLimitNotFoundException extends RuntimeException {
    public RiskLimitNotFoundException(String accountId) {
        super("Risk limits not found for account: " + accountId);
    }
}
