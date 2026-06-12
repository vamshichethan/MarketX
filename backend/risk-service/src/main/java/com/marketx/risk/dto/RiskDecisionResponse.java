package com.marketx.risk.dto;

import com.marketx.risk.enums.OrderSide;
import com.marketx.risk.enums.OrderType;
import com.marketx.risk.enums.RiskDecision;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RiskDecisionResponse(
        String decisionId,
        String accountId,
        String symbol,
        OrderSide side,
        OrderType type,
        int quantity,
        BigDecimal price,
        boolean approved,
        RiskDecision decision,
        List<String> reasons,
        LocalDateTime checkedAt
) {
}
