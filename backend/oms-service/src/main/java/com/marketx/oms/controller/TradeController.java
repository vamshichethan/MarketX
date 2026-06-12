package com.marketx.oms.controller;

import com.marketx.oms.dto.TradeResponse;
import com.marketx.oms.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/trades")
public class TradeController {
    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping
    public ResponseEntity<List<TradeResponse>> getTrades(@RequestParam(required = false) String symbol) {
        tradeService.syncTradesFromExchange();
        return ResponseEntity.ok(tradeService.getTrades(symbol));
    }
}
