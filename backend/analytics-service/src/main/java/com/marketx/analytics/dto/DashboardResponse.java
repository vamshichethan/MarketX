package com.marketx.analytics.dto;

import java.util.List;

public record DashboardResponse(List<SymbolDashboardRow> symbols) {
}
