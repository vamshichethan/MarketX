package com.marketx.risk.controller;

import com.marketx.risk.dto.EvaluateRiskRequest;
import com.marketx.risk.dto.MarketPriceRequest;
import com.marketx.risk.dto.RiskDecisionResponse;
import com.marketx.risk.dto.RiskEvaluationResponse;
import com.marketx.risk.dto.RiskLimitRequest;
import com.marketx.risk.dto.RiskLimitResponse;
import com.marketx.risk.service.RiskLimitService;
import com.marketx.risk.service.RiskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/risk")
public class RiskController {
    private final RiskService riskService;
    private final RiskLimitService riskLimitService;

    public RiskController(RiskService riskService, RiskLimitService riskLimitService) {
        this.riskService = riskService;
        this.riskLimitService = riskLimitService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<RiskEvaluationResponse> evaluate(@Valid @RequestBody EvaluateRiskRequest request) {
        return ResponseEntity.ok(riskService.evaluate(request));
    }

    @PutMapping("/limits/{accountId}")
    public ResponseEntity<RiskLimitResponse> updateLimits(
            @PathVariable String accountId,
            @RequestBody RiskLimitRequest request
    ) {
        return ResponseEntity.ok(riskLimitService.updateLimits(accountId, request));
    }

    @GetMapping("/limits/{accountId}")
    public ResponseEntity<RiskLimitResponse> getLimits(@PathVariable String accountId) {
        return ResponseEntity.ok(riskLimitService.getLimits(accountId));
    }

    @GetMapping("/decisions/{accountId}")
    public ResponseEntity<List<RiskDecisionResponse>> getDecisions(@PathVariable String accountId) {
        return ResponseEntity.ok(riskService.getDecisions(accountId));
    }

    @PostMapping("/market-price")
    public ResponseEntity<Void> updateMarketPrice(@Valid @RequestBody MarketPriceRequest request) {
        riskService.updateMarketPrice(request);
        return ResponseEntity.ok().build();
    }
}
