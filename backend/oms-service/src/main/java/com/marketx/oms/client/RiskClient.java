package com.marketx.oms.client;

import com.marketx.oms.dto.CreateOrderRequest;
import com.marketx.oms.dto.RiskEvaluationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class RiskClient {
    private final RestClient restClient;

    public RiskClient(@Value("${marketx.risk-service.url}") String riskServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(riskServiceUrl)
                .build();
    }

    public RiskEvaluationResponse evaluate(CreateOrderRequest request) {
        try {
            RiskEvaluationResponse response = restClient.post()
                    .uri("/risk/evaluate")
                    .body(request)
                    .retrieve()
                    .body(RiskEvaluationResponse.class);
            if (response == null) {
                return rejected(request, "Risk Service returned an empty response");
            }
            return response;
        } catch (RestClientException exception) {
            return rejected(request, "Risk Service unavailable");
        }
    }

    private RiskEvaluationResponse rejected(CreateOrderRequest request, String reason) {
        return new RiskEvaluationResponse(
                false,
                "REJECTED",
                List.of(reason),
                request.accountId(),
                request.symbol()
        );
    }
}
