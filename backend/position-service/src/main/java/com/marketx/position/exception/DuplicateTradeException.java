package com.marketx.position.exception;

public class DuplicateTradeException extends RuntimeException {
    public DuplicateTradeException(String tradeId) {
        super("Trade already processed: " + tradeId);
    }
}
