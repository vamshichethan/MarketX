package com.marketx.fixgateway.controller;

import com.marketx.fixgateway.dto.FixExecutionReportResponse;
import com.marketx.fixgateway.dto.FixMessageRequest;
import com.marketx.fixgateway.dto.FixMessageResponse;
import com.marketx.fixgateway.service.FixGatewayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fix")
public class FixGatewayController {
    private final FixGatewayService fixGatewayService;

    public FixGatewayController(FixGatewayService fixGatewayService) {
        this.fixGatewayService = fixGatewayService;
    }

    @PostMapping("/messages")
    public FixMessageResponse submitFixMessage(@RequestBody FixMessageRequest request) {
        return fixGatewayService.processInbound(request.message());
    }

    @GetMapping("/reports")
    public List<FixExecutionReportResponse> getReports() {
        return fixGatewayService.getReports();
    }

    @GetMapping("/reports/{clOrdId}")
    public List<FixExecutionReportResponse> getReportsForOrder(@PathVariable String clOrdId) {
        return fixGatewayService.getReportsForOrder(clOrdId);
    }
}
