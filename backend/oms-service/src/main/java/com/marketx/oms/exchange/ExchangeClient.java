package com.marketx.oms.exchange;

import com.marketx.oms.dto.CancelOrderResponse;
import com.marketx.oms.dto.CreateOrderRequest;
import com.marketx.oms.dto.ModifyOrderRequest;
import com.marketx.oms.dto.OrderBookResponse;
import com.marketx.oms.dto.OrderResponse;
import com.marketx.oms.dto.TradeResponse;

import java.util.List;
import java.util.Optional;

public interface ExchangeClient {
    OrderResponse placeOrder(CreateOrderRequest request);

    OrderResponse placeOrder(String orderId, CreateOrderRequest request);

    OrderResponse modifyOrder(String orderId, ModifyOrderRequest request);

    CancelOrderResponse cancelOrder(String orderId);

    OrderBookResponse getOrderBook(String symbol);

    List<TradeResponse> getTrades(String symbol);

    Optional<OrderResponse> getOrder(String orderId);
}
