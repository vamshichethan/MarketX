package com.marketx.risk.exception;

public class InvalidRiskRequestException extends RuntimeException {
    public InvalidRiskRequestException(String message) {
        super(message);
    }
}
