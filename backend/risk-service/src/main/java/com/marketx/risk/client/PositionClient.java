package com.marketx.risk.client;

import com.marketx.risk.dto.PositionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

@Component
public class PositionClient {
    private final RestClient restClient;

    public PositionClient(@Value("${marketx.position-service.url}") String positionServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(positionServiceUrl)
                .build();
    }

    public PositionLookupResult getPosition(String accountId, String symbol) {
        try {
            return new PositionLookupResult(Optional.ofNullable(restClient.get()
                    .uri("/positions/{accountId}/{symbol}", accountId, symbol)
                    .retrieve()
                    .body(PositionResponse.class)), true);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return new PositionLookupResult(Optional.empty(), true);
            }
            return new PositionLookupResult(Optional.empty(), false);
        } catch (RestClientException exception) {
            return new PositionLookupResult(Optional.empty(), false);
        }
    }

    public record PositionLookupResult(Optional<PositionResponse> position, boolean available) {
    }
}
