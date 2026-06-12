package com.marketx.pnl.exception;

public class PnlNotFoundException extends RuntimeException {
    public PnlNotFoundException(String accountId, String symbol) {
        super("PnL not found for account " + accountId + " and symbol " + symbol);
    }
}
