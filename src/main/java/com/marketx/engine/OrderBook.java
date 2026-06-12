package com.marketx.engine;

import com.marketx.model.Order;
import com.marketx.model.OrderSide;
import com.marketx.model.OrderType;
import com.marketx.model.Trade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.LongSupplier;

public class OrderBook {
    private final String symbol;
    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.buyOrders = new PriorityQueue<>(
                Comparator.comparing(Order::getPrice, Comparator.reverseOrder())
                        .thenComparing(Order::getTimestamp)
                        .thenComparingLong(Order::getOrderId)
        );
        this.sellOrders = new PriorityQueue<>(
                Comparator.comparing(Order::getPrice)
                        .thenComparing(Order::getTimestamp)
                        .thenComparingLong(Order::getOrderId)
        );
    }

    public MatchResult placeOrder(Order incomingOrder, LongSupplier tradeIdSupplier) {
        List<Trade> executedTrades = new ArrayList<>();

        if (incomingOrder.getSide() == OrderSide.BUY) {
            matchBuyOrder(incomingOrder, tradeIdSupplier, executedTrades);
            storeRemainingLimitOrder(incomingOrder, buyOrders);
        } else {
            matchSellOrder(incomingOrder, tradeIdSupplier, executedTrades);
            storeRemainingLimitOrder(incomingOrder, sellOrders);
        }

        return new MatchResult(executedTrades, incomingOrder.getQuantity());
    }

    public List<Order> getBuyOrdersSnapshot() {
        return getSortedSnapshot(buyOrders);
    }

    public List<Order> getSellOrdersSnapshot() {
        return getSortedSnapshot(sellOrders);
    }

    public String getSymbol() {
        return symbol;
    }

    private void matchBuyOrder(Order buyOrder, LongSupplier tradeIdSupplier, List<Trade> executedTrades) {
        while (!buyOrder.isFilled() && !sellOrders.isEmpty()) {
            Order bestSellOrder = sellOrders.peek();

            if (!canBuyOrderMatch(buyOrder, bestSellOrder)) {
                break;
            }

            executeTrade(buyOrder, bestSellOrder, bestSellOrder.getPrice(), tradeIdSupplier, executedTrades);

            if (bestSellOrder.isFilled()) {
                sellOrders.poll();
            }
        }
    }

    private void matchSellOrder(Order sellOrder, LongSupplier tradeIdSupplier, List<Trade> executedTrades) {
        while (!sellOrder.isFilled() && !buyOrders.isEmpty()) {
            Order bestBuyOrder = buyOrders.peek();

            if (!canSellOrderMatch(sellOrder, bestBuyOrder)) {
                break;
            }

            executeTrade(bestBuyOrder, sellOrder, bestBuyOrder.getPrice(), tradeIdSupplier, executedTrades);

            if (bestBuyOrder.isFilled()) {
                buyOrders.poll();
            }
        }
    }

    private boolean canBuyOrderMatch(Order buyOrder, Order sellOrder) {
        return buyOrder.getOrderType() == OrderType.MARKET
                || buyOrder.getPrice().compareTo(sellOrder.getPrice()) >= 0;
    }

    private boolean canSellOrderMatch(Order sellOrder, Order buyOrder) {
        return sellOrder.getOrderType() == OrderType.MARKET
                || sellOrder.getPrice().compareTo(buyOrder.getPrice()) <= 0;
    }

    private void executeTrade(
            Order buyOrder,
            Order sellOrder,
            BigDecimal tradePrice,
            LongSupplier tradeIdSupplier,
            List<Trade> executedTrades
    ) {
        int executedQuantity = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());

        buyOrder.reduceQuantity(executedQuantity);
        sellOrder.reduceQuantity(executedQuantity);

        executedTrades.add(new Trade(
                tradeIdSupplier.getAsLong(),
                buyOrder.getOrderId(),
                sellOrder.getOrderId(),
                symbol,
                executedQuantity,
                tradePrice,
                LocalDateTime.now()
        ));
    }

    private void storeRemainingLimitOrder(Order order, PriorityQueue<Order> targetQueue) {
        if (order.getOrderType() == OrderType.LIMIT && !order.isFilled()) {
            targetQueue.add(order);
        }
    }

    private List<Order> getSortedSnapshot(PriorityQueue<Order> orders) {
        List<Order> snapshot = new ArrayList<>(orders);
        snapshot.sort(orders.comparator());
        return snapshot;
    }

    public static class MatchResult {
        private final List<Trade> trades;
        private final int remainingQuantity;

        public MatchResult(List<Trade> trades, int remainingQuantity) {
            this.trades = trades;
            this.remainingQuantity = remainingQuantity;
        }

        public List<Trade> getTrades() {
            return trades;
        }

        public int getRemainingQuantity() {
            return remainingQuantity;
        }
    }
}
