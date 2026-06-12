package com.marketx.pnl.controller;

import com.marketx.pnl.dto.MarketPriceUpdateRequest;
import com.marketx.pnl.dto.PnlResponse;
import com.marketx.pnl.dto.PnlUpdateResult;
import com.marketx.pnl.dto.TradeEventRequest;
import com.marketx.pnl.service.PnlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pnl")
public class PnlController {
    private final PnlService pnlService;

    public PnlController(PnlService pnlService) {
        this.pnlService = pnlService;
    }

    @PostMapping("/events/trade")
    public ResponseEntity<PnlResponse> processTrade(@Valid @RequestBody TradeEventRequest request) {
        PnlUpdateResult result = pnlService.processTrade(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.pnl());
    }

    @PostMapping("/events/market-price")
    public ResponseEntity<List<PnlResponse>> updateMarketPrice(@Valid @RequestBody MarketPriceUpdateRequest request) {
        return ResponseEntity.ok(pnlService.updateMarketPrice(request));
    }

    @GetMapping("/{accountId}/{symbol}")
    public ResponseEntity<PnlResponse> getPnl(
            @PathVariable String accountId,
            @PathVariable String symbol
    ) {
        return ResponseEntity.ok(pnlService.getPnl(accountId, symbol));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<PnlResponse>> getPnlForAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(pnlService.getPnlForAccount(accountId));
    }

    @GetMapping
    public ResponseEntity<List<PnlResponse>> getAllPnl() {
        return ResponseEntity.ok(pnlService.getAllPnl());
    }
}
