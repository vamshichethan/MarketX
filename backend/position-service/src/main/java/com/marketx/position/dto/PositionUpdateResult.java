package com.marketx.position.dto;

public record PositionUpdateResult(
        PositionResponse position,
        boolean created
) {
}
