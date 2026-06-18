package com.marketx.replay.dto;

import com.marketx.replay.enums.ReplayStatus;

import java.time.LocalDateTime;

public record ReplaySessionResponse(
        String sessionId,
        String fileName,
        ReplayStatus status,
        int speedMultiplier,
        int currentIndex,
        int totalTicks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
