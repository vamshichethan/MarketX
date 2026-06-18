package com.marketx.replay.exception;

public class ReplaySessionNotFoundException extends RuntimeException {
    public ReplaySessionNotFoundException(String sessionId) {
        super("Replay session not found: " + sessionId);
    }
}
