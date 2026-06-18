package com.marketx.fixgateway.dto;

import java.time.LocalDateTime;

public record FixExecutionReportResponse(
        String clOrdId,
        String rawFixMessage,
        LocalDateTime createdAt
) {
}
