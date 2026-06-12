package com.marketx.risk.dto;

import com.marketx.risk.enums.RiskDecision;

import java.util.List;

public record RiskEvaluationResponse(
        boolean approved,
        RiskDecision decision,
        List<String> reasons,
        String accountId,
        String symbol
) {
}
