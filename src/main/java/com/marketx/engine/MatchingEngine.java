package com.marketx.engine;

import com.marketx.model.ExecutionReport;
import com.marketx.model.Order;
import com.marketx.model.OrderSide;
import com.marketx.model.OrderStatus;
import com.marketx.model.OrderType;
import com.marketx.model.Trade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MatchingEngine {
    private final AtomicLong nextOrderSequence = new AtomicLong(1);
    private final AtomicLong nextTradeSequence = new AtomicLong(1);
    private final AtomicLong nextExecutionSequence = new AtomicLong(1);
    private final Map<String, OrderBook> orderBooks = new LinkedHashMap<>();
    private final Map<String, Order> orderMap = new LinkedHashMap<>();
    private final Map<String, List<ExecutionReport>> reportsByOrderId = new LinkedHashMap<>();
    private final List<Trade> tradeHistory = new ArrayList<>();

    public OrderPlacementResult placeOrder(
            OrderSide side,
            OrderType orderType,
            String symbol,
            int quantity,
            BigDecimal price
    ) {
        String orderId = nextOrderId();
        String normalizedSymbol = normalizeSymbol(symbol);
        BigDecimal normalizedPrice = orderType == OrderType.MARKET ? BigDecimal.ZERO : price;

        Order order = new Order(
                orderId,
                side,
                orderType,
                normalizedSymbol,
                quantity,
                normalizedPrice == null ? BigDecimal.ZERO : normalizedPrice,
                LocalDateTime.now(),
                OrderStatus.NEW
        );

        String rejectionReason = validateOrder(orderType, quantity, price);
        if (rejectionReason != null) {
            order.setStatus(OrderStatus.REJECTED);
            orderMap.put(order.getOrderId(), order);
            addReport(order, OrderStatus.REJECTED, 0, BigDecimal.ZERO, rejectionReason);
            return OrderPlacementResult.rejected(order, rejectionReason);
        }

        orderMap.put(order.getOrderId(), order);
        addReport(order, OrderStatus.NEW, 0, BigDecimal.ZERO, "Order accepted");

        OrderBook orderBook = getOrCreateOrderBook(normalizedSymbol);
        OrderBook.MatchResult matchResult = orderBook.placeOrder(order, this::nextTradeId);
        tradeHistory.addAll(matchResult.getTrades());
        addFillReports(matchResult.getTrades());
        closeUnfilledMarketOrder(order);

        return OrderPlacementResult.accepted(order, matchResult.getTrades(), matchResult.getRemainingQuantity());
    }

    public EngineActionResult cancelOrder(String orderId) {
        Order order = orderMap.get(orderId);

        if (order == null) {
            return EngineActionResult.failure("ORDER NOT FOUND: " + orderId);
        }

        if (!order.isActive()) {
            return EngineActionResult.failure("ORDER CANNOT BE CANCELLED: " + orderId + " is " + order.getStatus());
        }

        if (order.getOrderType() != OrderType.LIMIT) {
            return EngineActionResult.failure("ORDER CANNOT BE CANCELLED: market orders are not stored in the book");
        }

        boolean removed = getOrCreateOrderBook(order.getSymbol()).removeOrder(order);
        if (!removed) {
            return EngineActionResult.failure("ORDER CANNOT BE CANCELLED: " + orderId + " is not resting in the book");
        }

        order.setStatus(OrderStatus.CANCELLED);
        addReport(order, OrderStatus.CANCELLED, 0, BigDecimal.ZERO, "Order cancelled");
        return EngineActionResult.success("ORDER CANCELLED: " + orderId, order);
    }

    public OrderPlacementResult modifyOrder(String orderId, int newQuantity, BigDecimal newPrice) {
        Order existingOrder = orderMap.get(orderId);

        if (existingOrder == null) {
            return OrderPlacementResult.failed("ORDER NOT FOUND: " + orderId);
        }

        if (!existingOrder.isActive()) {
            return OrderPlacementResult.failed("ORDER CANNOT BE MODIFIED: " + orderId + " is " + existingOrder.getStatus());
        }

        if (existingOrder.getOrderType() != OrderType.LIMIT) {
            return OrderPlacementResult.failed("ORDER CANNOT BE MODIFIED: only active limit orders can be modified");
        }

        String rejectionReason = validateOrder(OrderType.LIMIT, newQuantity, newPrice);
        if (rejectionReason != null) {
            addReport(existingOrder, OrderStatus.REJECTED, 0, BigDecimal.ZERO, rejectionReason);
            return OrderPlacementResult.failed("ORDER MODIFY REJECTED: " + rejectionReason);
        }

        getOrCreateOrderBook(existingOrder.getSymbol()).removeOrder(existingOrder);

        Order modifiedOrder = new Order(
                existingOrder.getOrderId(),
                existingOrder.getSide(),
                existingOrder.getOrderType(),
                existingOrder.getSymbol(),
                newQuantity,
                newPrice,
                LocalDateTime.now(),
                OrderStatus.NEW
        );

        orderMap.put(modifiedOrder.getOrderId(), modifiedOrder);
        addReport(modifiedOrder, OrderStatus.NEW, 0, BigDecimal.ZERO, "Order modified; time priority reset");

        OrderBook.MatchResult matchResult = getOrCreateOrderBook(modifiedOrder.getSymbol())
                .placeOrder(modifiedOrder, this::nextTradeId);
        tradeHistory.addAll(matchResult.getTrades());
        addFillReports(matchResult.getTrades());

        return OrderPlacementResult.modified(modifiedOrder, matchResult.getTrades(), matchResult.getRemainingQuantity());
    }

    public Order getOrder(String orderId) {
        return orderMap.get(orderId);
    }

    public List<ExecutionReport> getExecutionReports(String orderId) {
        return new ArrayList<>(reportsByOrderId.getOrDefault(orderId, Collections.emptyList()));
    }

    public OrderBook getOrCreateOrderBook(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        return orderBooks.computeIfAbsent(normalizedSymbol, OrderBook::new);
    }

    public List<Trade> getTradeHistory() {
        return new ArrayList<>(tradeHistory);
    }

    private String validateOrder(OrderType orderType, int quantity, BigDecimal price) {
        if (quantity <= 0) {
            return "Quantity must be greater than zero";
        }

        if (orderType == OrderType.LIMIT) {
            if (price == null) {
                return "Limit order requires a price";
            }

            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                return "Limit price must be greater than zero";
            }
        }

        if (orderType == OrderType.MARKET && price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            return "Market order price cannot be negative";
        }

        return null;
    }

    private void addFillReports(List<Trade> trades) {
        for (Trade trade : trades) {
            addFillReport(orderMap.get(trade.getBuyOrderId()), trade);
            addFillReport(orderMap.get(trade.getSellOrderId()), trade);
        }
    }

    private void addFillReport(Order order, Trade trade) {
        if (order == null) {
            return;
        }

        String message = order.getStatus() == OrderStatus.FILLED ? "Order filled" : "Order partially filled";
        addReport(order, order.getStatus(), trade.getQuantity(), trade.getPrice(), message);
    }

    private void closeUnfilledMarketOrder(Order order) {
        if (order.getOrderType() != OrderType.MARKET || order.isFilled()) {
            return;
        }

        if (order.getRemainingQuantity() == order.getOriginalQuantity()) {
            order.setStatus(OrderStatus.CANCELLED);
            addReport(order, OrderStatus.CANCELLED, 0, BigDecimal.ZERO, "Market order not filled");
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
            addReport(order, OrderStatus.PARTIALLY_FILLED, 0, BigDecimal.ZERO, "Market order has unfilled quantity");
        }
    }

    private void addReport(
            Order order,
            OrderStatus status,
            int executedQuantity,
            BigDecimal executedPrice,
            String message
    ) {
        ExecutionReport report = new ExecutionReport(
                nextExecutionId(),
                order.getOrderId(),
                order.getSymbol(),
                order.getSide(),
                status,
                executedQuantity,
                executedPrice,
                order.getRemainingQuantity(),
                message,
                LocalDateTime.now()
        );

        reportsByOrderId.computeIfAbsent(order.getOrderId(), ignored -> new ArrayList<>()).add(report);
    }

    private String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.toUpperCase();
    }

    private String nextOrderId() {
        return "ORD-" + nextOrderSequence.getAndIncrement();
    }

    private String nextTradeId() {
        return "TRD-" + nextTradeSequence.getAndIncrement();
    }

    private String nextExecutionId() {
        return "EXE-" + nextExecutionSequence.getAndIncrement();
    }

    public static class OrderPlacementResult {
        private final boolean success;
        private final boolean rejected;
        private final boolean modified;
        private final String message;
        private final Order order;
        private final List<Trade> trades;
        private final int remainingQuantity;

        private OrderPlacementResult(
                boolean success,
                boolean rejected,
                boolean modified,
                String message,
                Order order,
                List<Trade> trades,
                int remainingQuantity
        ) {
            this.success = success;
            this.rejected = rejected;
            this.modified = modified;
            this.message = message;
            this.order = order;
            this.trades = trades;
            this.remainingQuantity = remainingQuantity;
        }

        public static OrderPlacementResult accepted(Order order, List<Trade> trades, int remainingQuantity) {
            return new OrderPlacementResult(true, false, false, "Order accepted", order, trades, remainingQuantity);
        }

        public static OrderPlacementResult modified(Order order, List<Trade> trades, int remainingQuantity) {
            return new OrderPlacementResult(true, false, true, "Order modified", order, trades, remainingQuantity);
        }

        public static OrderPlacementResult rejected(Order order, String reason) {
            return new OrderPlacementResult(false, true, false, reason, order, Collections.emptyList(), order.getRemainingQuantity());
        }

        public static OrderPlacementResult failed(String message) {
            return new OrderPlacementResult(false, false, false, message, null, Collections.emptyList(), 0);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isRejected() {
            return rejected;
        }

        public boolean isModified() {
            return modified;
        }

        public String getMessage() {
            return message;
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

    public static class EngineActionResult {
        private final boolean success;
        private final String message;
        private final Order order;

        private EngineActionResult(boolean success, String message, Order order) {
            this.success = success;
            this.message = message;
            this.order = order;
        }

        public static EngineActionResult success(String message, Order order) {
            return new EngineActionResult(true, message, order);
        }

        public static EngineActionResult failure(String message) {
            return new EngineActionResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Order getOrder() {
            return order;
        }
    }
}
