package com.marketx.replay.dto;

import jakarta.validation.constraints.Min;

public record SpeedUpdateRequest(@Min(1) int speedMultiplier) {
}
