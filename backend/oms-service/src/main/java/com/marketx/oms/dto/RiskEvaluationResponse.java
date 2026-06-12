package com.marketx.oms.dto;

import java.util.List;

public record RiskEvaluationResponse(
        boolean approved,
        String decision,
        List<String> reasons,
        String accountId,
        String symbol
) {
}
