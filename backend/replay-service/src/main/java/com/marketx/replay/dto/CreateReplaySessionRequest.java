package com.marketx.replay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateReplaySessionRequest(
        @NotBlank String filePath,
        @Min(1) int speedMultiplier
) {
}
