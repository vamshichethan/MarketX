package com.marketx.risk.client;

import com.marketx.risk.dto.PnlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class PnlClient {
    private final RestClient restClient;

    public PnlClient(@Value("${marketx.pnl-service.url}") String pnlServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(pnlServiceUrl)
                .build();
    }

    public Optional<BigDecimal> getTotalPnlForAccount(String accountId) {
        try {
            List<PnlResponse> rows = restClient.get()
                    .uri("/pnl/{accountId}", accountId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PnlResponse>>() {
                    });

            if (rows == null) {
                return Optional.empty();
            }

            return Optional.of(rows.stream()
                    .map(PnlResponse::totalPnl)
                    .filter(value -> value != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }
}
