package com.marketx.fixgateway.dto;

public record FixMessageResponse(
        boolean accepted,
        String messageType,
        String description,
        String publishedTopic,
        String clientOrderId,
        String rejectionReason
) {
}
