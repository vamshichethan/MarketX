package com.marketx.position.exception;

public class PositionNotFoundException extends RuntimeException {
    public PositionNotFoundException(String accountId, String symbol) {
        super("Position not found for account " + accountId + " and symbol " + symbol);
    }
}
