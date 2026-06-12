package com.marketx.oms.client;

import com.marketx.oms.dto.TradeResponse;
import com.marketx.oms.enums.OrderSide;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class PositionClient {
    private final RestClient restClient;

    public PositionClient(
            RestClient.Builder restClientBuilder,
            @Value("${marketx.position-service.url:http://localhost:8081}") String positionServiceUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(positionServiceUrl).build();
    }

    public void notifyTrade(TradeResponse trade) {
        sendTradeEvent(new TradeEventRequest(
                trade.tradeId() + "-BUY",
                trade.buyAccountId(),
                trade.symbol(),
                OrderSide.BUY,
                trade.quantity(),
                trade.price(),
                trade.executedAt()
        ));

        sendTradeEvent(new TradeEventRequest(
                trade.tradeId() + "-SELL",
                trade.sellAccountId(),
                trade.symbol(),
                OrderSide.SELL,
                trade.quantity(),
                trade.price(),
                trade.executedAt()
        ));
    }

    private void sendTradeEvent(TradeEventRequest request) {
        try {
            restClient.post()
                    .uri("/positions/events/trade")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (httpRequest, response) -> {
                        if (response.getStatusCode().value() != 409) {
                            throw new RestClientException("Position Service rejected trade event");
                        }
                    })
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            System.out.println("Position Service notification failed: " + exception.getMessage());
        }
    }

    private record TradeEventRequest(
            String tradeId,
            String accountId,
            String symbol,
            OrderSide side,
            int quantity,
            BigDecimal price,
            LocalDateTime executedAt
    ) {
    }
}
