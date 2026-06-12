package com.marketx.engine;

import com.marketx.model.Order;
import com.marketx.model.OrderSide;
import com.marketx.model.OrderType;
import com.marketx.model.Trade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MatchingEngine {
    private final AtomicLong nextOrderId = new AtomicLong(1);
    private final AtomicLong nextTradeId = new AtomicLong(1);
    private final Map<String, OrderBook> orderBooks = new LinkedHashMap<>();
    private final List<Trade> tradeHistory = new ArrayList<>();

    public OrderPlacementResult placeOrder(
            OrderSide side,
            OrderType orderType,
            String symbol,
            int quantity,
            BigDecimal price
    ) {
        String normalizedSymbol = symbol.toUpperCase();
        BigDecimal normalizedPrice = orderType == OrderType.MARKET ? BigDecimal.ZERO : price;

        Order order = new Order(
                nextOrderId.getAndIncrement(),
                side,
                orderType,
                normalizedSymbol,
                quantity,
                normalizedPrice,
                LocalDateTime.now()
        );

        OrderBook orderBook = getOrCreateOrderBook(normalizedSymbol);
        OrderBook.MatchResult matchResult = orderBook.placeOrder(order, nextTradeId::getAndIncrement);
        tradeHistory.addAll(matchResult.getTrades());

        return new OrderPlacementResult(order, matchResult.getTrades(), matchResult.getRemainingQuantity());
    }

    public OrderBook getOrCreateOrderBook(String symbol) {
        String normalizedSymbol = symbol.toUpperCase();
        return orderBooks.computeIfAbsent(normalizedSymbol, OrderBook::new);
    }

    public List<Trade> getTradeHistory() {
        return new ArrayList<>(tradeHistory);
    }

    public static class OrderPlacementResult {
        private final Order order;
        private final List<Trade> trades;
        private final int remainingQuantity;

        public OrderPlacementResult(Order order, List<Trade> trades, int remainingQuantity) {
            this.order = order;
            this.trades = trades;
            this.remainingQuantity = remainingQuantity;
        }

        public Order getOrder() {
            return order;
        }

        public List<Trade> getTrades() {
            return trades;
        }

        public int getRemainingQuantity() {
            return remainingQuantity;
        }
    }
}
