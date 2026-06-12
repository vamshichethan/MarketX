package com.marketx.oms.service;

import com.marketx.oms.dto.OrderBookResponse;
import com.marketx.oms.exchange.ExchangeClient;
import org.springframework.stereotype.Service;

@Service
public class OrderBookService {
    private final ExchangeClient exchangeClient;

    public OrderBookService(ExchangeClient exchangeClient) {
        this.exchangeClient = exchangeClient;
    }

    public OrderBookResponse getOrderBook(String symbol) {
        return exchangeClient.getOrderBook(symbol);
    }
}
