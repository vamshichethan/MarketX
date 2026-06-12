package com.marketx.engine;

import com.marketx.model.Order;
import com.marketx.model.OrderSide;
import com.marketx.model.OrderStatus;
import com.marketx.model.OrderType;
import com.marketx.model.Trade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Supplier;
import java.util.TreeMap;

public class OrderBook {
    private final String symbol;
    private final TreeMap<BigDecimal, Queue<Order>> buyLevels;
    private final TreeMap<BigDecimal, Queue<Order>> sellLevels;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.buyLevels = new TreeMap<>(Comparator.reverseOrder());
        this.sellLevels = new TreeMap<>();
    }

    public MatchResult placeOrder(Order incomingOrder, Supplier<String> tradeIdSupplier) {
        List<Trade> executedTrades = new ArrayList<>();

        if (incomingOrder.getSide() == OrderSide.BUY) {
            matchBuyOrder(incomingOrder, tradeIdSupplier, executedTrades);
            storeRemainingLimitOrder(incomingOrder, buyLevels);
        } else {
            matchSellOrder(incomingOrder, tradeIdSupplier, executedTrades);
            storeRemainingLimitOrder(incomingOrder, sellLevels);
        }

        return new MatchResult(executedTrades, incomingOrder.getRemainingQuantity());
    }

    public boolean removeOrder(Order order) {
        TreeMap<BigDecimal, Queue<Order>> levels = order.getSide() == OrderSide.BUY ? buyLevels : sellLevels;
        Queue<Order> ordersAtPrice = levels.get(order.getPrice());

        if (ordersAtPrice == null) {
            return false;
        }

        boolean removed = ordersAtPrice.remove(order);
        if (ordersAtPrice.isEmpty()) {
            levels.remove(order.getPrice());
        }

        return removed;
    }

    public List<OrderBookLevel> getTopBuyLevels(int maxLevels) {
        return getTopLevels(buyLevels, maxLevels);
    }

    public List<OrderBookLevel> getTopSellLevels(int maxLevels) {
        return getTopLevels(sellLevels, maxLevels);
    }

    public BigDecimal getBestBid() {
        return buyLevels.isEmpty() ? null : buyLevels.firstKey();
    }

    public BigDecimal getBestAsk() {
        return sellLevels.isEmpty() ? null : sellLevels.firstKey();
    }

    public BigDecimal getSpread() {
        BigDecimal bestBid = getBestBid();
        BigDecimal bestAsk = getBestAsk();

        if (bestBid == null || bestAsk == null) {
            return null;
        }

        return bestAsk.subtract(bestBid);
    }

    public String getSymbol() {
        return symbol;
    }

    private void matchBuyOrder(Order buyOrder, Supplier<String> tradeIdSupplier, List<Trade> executedTrades) {
        // Sell levels are sorted lowest price first, so firstEntry is the best ask.
        while (!buyOrder.isFilled() && !sellLevels.isEmpty()) {
            Map.Entry<BigDecimal, Queue<Order>> bestAskLevel = sellLevels.firstEntry();
            BigDecimal bestAskPrice = bestAskLevel.getKey();

            if (!canBuyOrderMatch(buyOrder, bestAskPrice)) {
                break;
            }

            Queue<Order> sellOrdersAtBestPrice = bestAskLevel.getValue();
            Order restingSellOrder = sellOrdersAtBestPrice.peek();

            executeTrade(buyOrder, restingSellOrder, bestAskPrice, OrderSide.BUY, tradeIdSupplier, executedTrades);

            if (restingSellOrder.isFilled()) {
                sellOrdersAtBestPrice.poll();
            }

            if (sellOrdersAtBestPrice.isEmpty()) {
                sellLevels.pollFirstEntry();
            }
        }
    }

    private void matchSellOrder(Order sellOrder, Supplier<String> tradeIdSupplier, List<Trade> executedTrades) {
        // Buy levels are sorted highest price first, so firstEntry is the best bid.
        while (!sellOrder.isFilled() && !buyLevels.isEmpty()) {
            Map.Entry<BigDecimal, Queue<Order>> bestBidLevel = buyLevels.firstEntry();
            BigDecimal bestBidPrice = bestBidLevel.getKey();

            if (!canSellOrderMatch(sellOrder, bestBidPrice)) {
                break;
            }

            Queue<Order> buyOrdersAtBestPrice = bestBidLevel.getValue();
            Order restingBuyOrder = buyOrdersAtBestPrice.peek();

            executeTrade(restingBuyOrder, sellOrder, bestBidPrice, OrderSide.SELL, tradeIdSupplier, executedTrades);

            if (restingBuyOrder.isFilled()) {
                buyOrdersAtBestPrice.poll();
            }

            if (buyOrdersAtBestPrice.isEmpty()) {
                buyLevels.pollFirstEntry();
            }
        }
    }

    private boolean canBuyOrderMatch(Order buyOrder, BigDecimal bestAskPrice) {
        return buyOrder.getOrderType() == OrderType.MARKET
                || buyOrder.getPrice().compareTo(bestAskPrice) >= 0;
    }

    private boolean canSellOrderMatch(Order sellOrder, BigDecimal bestBidPrice) {
        return sellOrder.getOrderType() == OrderType.MARKET
                || sellOrder.getPrice().compareTo(bestBidPrice) <= 0;
    }

    private void executeTrade(
            Order buyOrder,
            Order sellOrder,
            BigDecimal restingOrderPrice,
            OrderSide aggressorSide,
            Supplier<String> tradeIdSupplier,
            List<Trade> executedTrades
    ) {
        int executedQuantity = Math.min(buyOrder.getRemainingQuantity(), sellOrder.getRemainingQuantity());

        buyOrder.reduceQuantity(executedQuantity);
        sellOrder.reduceQuantity(executedQuantity);
        updateStatusAfterExecution(buyOrder);
        updateStatusAfterExecution(sellOrder);

        executedTrades.add(new Trade(
                tradeIdSupplier.get(),
                buyOrder.getOrderId(),
                sellOrder.getOrderId(),
                symbol,
                executedQuantity,
                restingOrderPrice,
                aggressorSide,
                LocalDateTime.now()
        ));
    }

    private void updateStatusAfterExecution(Order order) {
        if (order.isFilled()) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
    }

    private void storeRemainingLimitOrder(Order order, TreeMap<BigDecimal, Queue<Order>> targetLevels) {
        if (order.getOrderType() == OrderType.LIMIT && !order.isFilled()) {
            if (order.getStatus() != OrderStatus.PARTIALLY_FILLED) {
                order.setStatus(OrderStatus.NEW);
            }
            targetLevels.computeIfAbsent(order.getPrice(), ignored -> new ArrayDeque<>()).add(order);
        }
    }

    private List<OrderBookLevel> getTopLevels(TreeMap<BigDecimal, Queue<Order>> levels, int maxLevels) {
        List<OrderBookLevel> topLevels = new ArrayList<>();

        for (Map.Entry<BigDecimal, Queue<Order>> entry : levels.entrySet()) {
            if (topLevels.size() == maxLevels) {
                break;
            }

            int totalQuantity = entry.getValue()
                    .stream()
                    .filter(Order::isActive)
                    .mapToInt(Order::getRemainingQuantity)
                    .sum();

            if (totalQuantity > 0) {
                topLevels.add(new OrderBookLevel(entry.getKey(), totalQuantity));
            }
        }

        return topLevels;
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
