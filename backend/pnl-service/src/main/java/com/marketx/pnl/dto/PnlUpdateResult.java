package com.marketx.pnl.dto;

public record PnlUpdateResult(
        PnlResponse pnl,
        boolean created
) {
}
