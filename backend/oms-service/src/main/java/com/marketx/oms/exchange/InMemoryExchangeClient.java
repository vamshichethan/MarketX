package com.marketx.oms.exchange;

import com.marketx.oms.dto.CancelOrderResponse;
import com.marketx.oms.dto.CreateOrderRequest;
import com.marketx.oms.dto.ModifyOrderRequest;
import com.marketx.oms.dto.OrderBookLevelResponse;
import com.marketx.oms.dto.OrderBookResponse;
import com.marketx.oms.dto.OrderResponse;
import com.marketx.oms.dto.TradeResponse;
import com.marketx.oms.enums.OrderSide;
import com.marketx.oms.enums.OrderStatus;
import com.marketx.oms.enums.OrderType;
import com.marketx.oms.exception.InvalidOrderException;
import com.marketx.oms.exception.InvalidOrderStateException;
import com.marketx.oms.exception.OrderNotFoundException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryExchangeClient implements ExchangeClient {
    private static final int TOP_LEVEL_COUNT = 5;

    private final AtomicLong nextOrderSequence = new AtomicLong(1);
    private final AtomicLong nextTradeSequence = new AtomicLong(1);
    private final Map<String, ExchangeOrder> ordersById = new LinkedHashMap<>();
    private final Map<String, ExchangeOrderBook> booksBySymbol = new LinkedHashMap<>();
    private final List<TradeResponse> trades = new ArrayList<>();

    @Override
    public synchronized OrderResponse placeOrder(CreateOrderRequest request) {
        validateCreate(request);

        String symbol = request.symbol().toUpperCase();
        BigDecimal price = request.type() == OrderType.MARKET ? BigDecimal.ZERO : request.price();
        ExchangeOrder order = new ExchangeOrder(
                nextOrderId(),
                symbol,
                request.side(),
                request.type(),
                request.quantity(),
                price,
                LocalDateTime.now()
        );

        ordersById.put(order.orderId, order);
        getBook(symbol).place(order);
        return toOrderResponse(order);
    }

    @Override
    public synchronized OrderResponse modifyOrder(String orderId, ModifyOrderRequest request) {
        ExchangeOrder existingOrder = findOrder(orderId);

        if (!existingOrder.isActive()) {
            throw new InvalidOrderStateException("Only active orders can be modified");
        }

        if (existingOrder.type != OrderType.LIMIT) {
            throw new InvalidOrderStateException("Only active LIMIT orders can be modified");
        }

        validateLimitQuantityAndPrice(request.quantity(), request.price());
        getBook(existingOrder.symbol).remove(existingOrder);

        ExchangeOrder modifiedOrder = new ExchangeOrder(
                existingOrder.orderId,
                existingOrder.symbol,
                existingOrder.side,
                existingOrder.type,
                request.quantity(),
                request.price(),
                LocalDateTime.now()
        );

        ordersById.put(modifiedOrder.orderId, modifiedOrder);
        getBook(modifiedOrder.symbol).place(modifiedOrder);
        return toOrderResponse(modifiedOrder);
    }

    @Override
    public synchronized CancelOrderResponse cancelOrder(String orderId) {
        ExchangeOrder order = findOrder(orderId);

        if (!order.isActive()) {
            throw new InvalidOrderStateException("Only active orders can be cancelled");
        }

        getBook(order.symbol).remove(order);
        order.status = OrderStatus.CANCELLED;
        order.updatedAt = LocalDateTime.now();
        return new CancelOrderResponse(order.orderId, order.status, "Order cancelled successfully");
    }

    @Override
    public synchronized OrderBookResponse getOrderBook(String symbol) {
        ExchangeOrderBook book = getBook(symbol.toUpperCase());
        BigDecimal bestBid = book.bestBid();
        BigDecimal bestAsk = book.bestAsk();
        BigDecimal spread = bestBid == null || bestAsk == null ? null : bestAsk.subtract(bestBid);

        return new OrderBookResponse(
                symbol.toUpperCase(),
                book.topBids(TOP_LEVEL_COUNT),
                book.topAsks(TOP_LEVEL_COUNT),
                bestBid,
                bestAsk,
                spread
        );
    }

    @Override
    public synchronized List<TradeResponse> getTrades(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return new ArrayList<>(trades);
        }

        String normalizedSymbol = symbol.toUpperCase();
        return trades.stream()
                .filter(trade -> trade.symbol().equals(normalizedSymbol))
                .toList();
    }

    @Override
    public synchronized Optional<OrderResponse> getOrder(String orderId) {
        return Optional.ofNullable(ordersById.get(orderId)).map(this::toOrderResponse);
    }

    private ExchangeOrder findOrder(String orderId) {
        ExchangeOrder order = ordersById.get(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        return order;
    }

    private ExchangeOrderBook getBook(String symbol) {
        return booksBySymbol.computeIfAbsent(symbol, ExchangeOrderBook::new);
    }

    private void validateCreate(CreateOrderRequest request) {
        if (request.symbol() == null || request.symbol().isBlank()) {
            throw new InvalidOrderException("Symbol is required");
        }

        if (request.side() == null) {
            throw new InvalidOrderException("Side is required");
        }

        if (request.type() == null) {
            throw new InvalidOrderException("Type is required");
        }

        if (request.type() == OrderType.LIMIT) {
            validateLimitQuantityAndPrice(request.quantity(), request.price());
            return;
        }

        if (request.quantity() <= 0) {
            throw new InvalidOrderException("Quantity must be greater than zero");
        }

        if (request.price() != null && request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOrderException("Market order price cannot be negative");
        }
    }

    private void validateLimitQuantityAndPrice(int quantity, BigDecimal price) {
        if (quantity <= 0) {
            throw new InvalidOrderException("Quantity must be greater than zero");
        }

        if (price == null) {
            throw new InvalidOrderException("LIMIT order requires price");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("LIMIT price must be greater than zero");
        }
    }

    private OrderResponse toOrderResponse(ExchangeOrder order) {
        return new OrderResponse(
                order.orderId,
                order.symbol,
                order.side,
                order.type,
                order.originalQuantity,
                order.remainingQuantity,
                order.price,
                order.status,
                order.createdAt,
                order.updatedAt
        );
    }

    private String nextOrderId() {
        return "ORD-" + nextOrderSequence.getAndIncrement();
    }

    private String nextTradeId() {
        return "TRD-" + nextTradeSequence.getAndIncrement();
    }

    private class ExchangeOrderBook {
        private final String symbol;
        private final TreeMap<BigDecimal, Queue<ExchangeOrder>> buyLevels = new TreeMap<>(Comparator.reverseOrder());
        private final TreeMap<BigDecimal, Queue<ExchangeOrder>> sellLevels = new TreeMap<>();

        private ExchangeOrderBook(String symbol) {
            this.symbol = symbol;
        }

        private void place(ExchangeOrder incomingOrder) {
            if (incomingOrder.side == OrderSide.BUY) {
                matchBuy(incomingOrder);
                storeRemainingLimitOrder(incomingOrder, buyLevels);
            } else {
                matchSell(incomingOrder);
                storeRemainingLimitOrder(incomingOrder, sellLevels);
            }

            if (incomingOrder.type == OrderType.MARKET && incomingOrder.remainingQuantity > 0) {
                incomingOrder.status = incomingOrder.remainingQuantity == incomingOrder.originalQuantity
                        ? OrderStatus.CANCELLED
                        : OrderStatus.PARTIALLY_FILLED;
                incomingOrder.updatedAt = LocalDateTime.now();
            }
        }

        private void remove(ExchangeOrder order) {
            TreeMap<BigDecimal, Queue<ExchangeOrder>> levels = order.side == OrderSide.BUY ? buyLevels : sellLevels;
            Queue<ExchangeOrder> ordersAtPrice = levels.get(order.price);
            if (ordersAtPrice == null) {
                return;
            }

            ordersAtPrice.remove(order);
            if (ordersAtPrice.isEmpty()) {
                levels.remove(order.price);
            }
        }

        private BigDecimal bestBid() {
            return buyLevels.isEmpty() ? null : buyLevels.firstKey();
        }

        private BigDecimal bestAsk() {
            return sellLevels.isEmpty() ? null : sellLevels.firstKey();
        }

        private List<OrderBookLevelResponse> topBids(int maxLevels) {
            return topLevels(buyLevels, maxLevels);
        }

        private List<OrderBookLevelResponse> topAsks(int maxLevels) {
            return topLevels(sellLevels, maxLevels);
        }

        private void matchBuy(ExchangeOrder buyOrder) {
            while (buyOrder.remainingQuantity > 0 && !sellLevels.isEmpty()) {
                Map.Entry<BigDecimal, Queue<ExchangeOrder>> bestAskLevel = sellLevels.firstEntry();
                BigDecimal bestAskPrice = bestAskLevel.getKey();

                if (buyOrder.type == OrderType.LIMIT && buyOrder.price.compareTo(bestAskPrice) < 0) {
                    break;
                }

                Queue<ExchangeOrder> ordersAtPrice = bestAskLevel.getValue();
                ExchangeOrder restingSellOrder = ordersAtPrice.peek();
                executeTrade(buyOrder, restingSellOrder, bestAskPrice, OrderSide.BUY);
                removeFilledRestingOrder(restingSellOrder, ordersAtPrice, sellLevels, bestAskPrice);
            }
        }

        private void matchSell(ExchangeOrder sellOrder) {
            while (sellOrder.remainingQuantity > 0 && !buyLevels.isEmpty()) {
                Map.Entry<BigDecimal, Queue<ExchangeOrder>> bestBidLevel = buyLevels.firstEntry();
                BigDecimal bestBidPrice = bestBidLevel.getKey();

                if (sellOrder.type == OrderType.LIMIT && sellOrder.price.compareTo(bestBidPrice) > 0) {
                    break;
                }

                Queue<ExchangeOrder> ordersAtPrice = bestBidLevel.getValue();
                ExchangeOrder restingBuyOrder = ordersAtPrice.peek();
                executeTrade(restingBuyOrder, sellOrder, bestBidPrice, OrderSide.SELL);
                removeFilledRestingOrder(restingBuyOrder, ordersAtPrice, buyLevels, bestBidPrice);
            }
        }

        private void executeTrade(
                ExchangeOrder buyOrder,
                ExchangeOrder sellOrder,
                BigDecimal restingPrice,
                OrderSide aggressorSide
        ) {
            int executedQuantity = Math.min(buyOrder.remainingQuantity, sellOrder.remainingQuantity);
            buyOrder.remainingQuantity -= executedQuantity;
            sellOrder.remainingQuantity -= executedQuantity;
            updateStatusAfterExecution(buyOrder);
            updateStatusAfterExecution(sellOrder);

            trades.add(new TradeResponse(
                    nextTradeId(),
                    symbol,
                    executedQuantity,
                    restingPrice,
                    buyOrder.orderId,
                    sellOrder.orderId,
                    aggressorSide,
                    LocalDateTime.now()
            ));
        }

        private void updateStatusAfterExecution(ExchangeOrder order) {
            order.status = order.remainingQuantity == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
            order.updatedAt = LocalDateTime.now();
        }

        private void removeFilledRestingOrder(
                ExchangeOrder restingOrder,
                Queue<ExchangeOrder> ordersAtPrice,
                TreeMap<BigDecimal, Queue<ExchangeOrder>> levels,
                BigDecimal price
        ) {
            if (restingOrder.remainingQuantity == 0) {
                ordersAtPrice.poll();
            }

            if (ordersAtPrice.isEmpty()) {
                levels.remove(price);
            }
        }

        private void storeRemainingLimitOrder(
                ExchangeOrder order,
                TreeMap<BigDecimal, Queue<ExchangeOrder>> levels
        ) {
            if (order.type == OrderType.LIMIT && order.remainingQuantity > 0) {
                levels.computeIfAbsent(order.price, ignored -> new ArrayDeque<>()).add(order);
            }
        }

        private List<OrderBookLevelResponse> topLevels(
                TreeMap<BigDecimal, Queue<ExchangeOrder>> levels,
                int maxLevels
        ) {
            List<OrderBookLevelResponse> topLevels = new ArrayList<>();
            for (Map.Entry<BigDecimal, Queue<ExchangeOrder>> entry : levels.entrySet()) {
                if (topLevels.size() == maxLevels) {
                    break;
                }

                int quantity = entry.getValue().stream()
                        .filter(ExchangeOrder::isActive)
                        .mapToInt(order -> order.remainingQuantity)
                        .sum();

                if (quantity > 0) {
                    topLevels.add(new OrderBookLevelResponse(entry.getKey(), quantity));
                }
            }
            return topLevels;
        }
    }

    private static class ExchangeOrder {
        private final String orderId;
        private final String symbol;
        private final OrderSide side;
        private final OrderType type;
        private final int originalQuantity;
        private int remainingQuantity;
        private final BigDecimal price;
        private OrderStatus status = OrderStatus.NEW;
        private final LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private ExchangeOrder(
                String orderId,
                String symbol,
                OrderSide side,
                OrderType type,
                int originalQuantity,
                BigDecimal price,
                LocalDateTime createdAt
        ) {
            this.orderId = orderId;
            this.symbol = symbol;
            this.side = side;
            this.type = type;
            this.originalQuantity = originalQuantity;
            this.remainingQuantity = originalQuantity;
            this.price = price;
            this.createdAt = createdAt;
            this.updatedAt = createdAt;
        }

        private boolean isActive() {
            return type == OrderType.LIMIT
                    && (status == OrderStatus.NEW || status == OrderStatus.PARTIALLY_FILLED);
        }
    }
}
