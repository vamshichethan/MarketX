package com.marketx.position.controller;

import com.marketx.position.dto.PositionResponse;
import com.marketx.position.dto.PositionUpdateResult;
import com.marketx.position.dto.TradeEventRequest;
import com.marketx.position.service.PositionService;
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
@RequestMapping("/positions")
public class PositionController {
    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @PostMapping("/events/trade")
    public ResponseEntity<PositionResponse> processTrade(@Valid @RequestBody TradeEventRequest request) {
        PositionUpdateResult result = positionService.processTrade(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.position());
    }

    @GetMapping("/{accountId}/{symbol}")
    public ResponseEntity<PositionResponse> getPosition(
            @PathVariable String accountId,
            @PathVariable String symbol
    ) {
        return ResponseEntity.ok(positionService.getPosition(accountId, symbol));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<PositionResponse>> getPositionsForAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(positionService.getPositionsForAccount(accountId));
    }

    @GetMapping
    public ResponseEntity<List<PositionResponse>> getAllPositions() {
        return ResponseEntity.ok(positionService.getAllPositions());
    }
}
