package com.marketx.oms.service;

import com.marketx.common.events.OrderRiskApprovedEvent;
import com.marketx.common.events.OrderRiskRejectedEvent;
import com.marketx.common.events.OrderSubmittedEvent;
import com.marketx.oms.client.RiskClient;
import com.marketx.oms.dto.CancelOrderResponse;
import com.marketx.oms.dto.CreateOrderRequest;
import com.marketx.oms.dto.ModifyOrderRequest;
import com.marketx.oms.dto.OrderResponse;
import com.marketx.oms.dto.RiskEvaluationResponse;
import com.marketx.oms.dto.TradeResponse;
import com.marketx.oms.entity.ExecutionReportEntity;
import com.marketx.oms.entity.OrderEntity;
import com.marketx.oms.enums.OrderStatus;
import com.marketx.oms.enums.OrderType;
import com.marketx.oms.exception.InvalidOrderException;
import com.marketx.oms.exception.InvalidOrderStateException;
import com.marketx.oms.exception.OrderNotFoundException;
import com.marketx.oms.exchange.ExchangeClient;
import com.marketx.oms.kafka.OrderEventPublisher;
import com.marketx.oms.repository.ExecutionReportRepository;
import com.marketx.oms.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderService {
    private final AtomicLong nextExecutionSequence = new AtomicLong(1);
    private final AtomicLong nextOrderSequence = new AtomicLong(1);
    private final OrderRepository orderRepository;
    private final ExecutionReportRepository executionReportRepository;
    private final ExchangeClient exchangeClient;
    private final TradeService tradeService;
    private final RiskClient riskClient;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(
            OrderRepository orderRepository,
            ExecutionReportRepository executionReportRepository,
            ExchangeClient exchangeClient,
            TradeService tradeService,
            RiskClient riskClient,
            OrderEventPublisher orderEventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.executionReportRepository = executionReportRepository;
        this.exchangeClient = exchangeClient;
        this.tradeService = tradeService;
        this.riskClient = riskClient;
        this.orderEventPublisher = orderEventPublisher;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        validateCreateOrder(request);
        CreateOrderRequest normalizedRequest = normalizeCreateRequest(request);
        OrderEntity pendingOrder = savePendingRiskOrder(normalizedRequest);
        addExecutionReport(pendingOrder, 0, BigDecimal.ZERO, "Order submitted for risk evaluation");
        orderEventPublisher.publishOrderSubmitted(toOrderSubmittedEvent(pendingOrder));
        return toResponse(pendingOrder, "Order submitted for risk evaluation");
    }

    @Transactional
    public OrderResponse modifyOrder(String orderId, ModifyOrderRequest request) {
        OrderEntity currentOrder = findOrder(orderId);
        validateModify(currentOrder, request);
        RiskEvaluationResponse riskResponse = riskClient.evaluate(new CreateOrderRequest(
                currentOrder.getAccountId(),
                currentOrder.getSymbol(),
                currentOrder.getSide(),
                currentOrder.getType(),
                request.quantity(),
                request.price()
        ));
        if (!riskResponse.approved()) {
            addExecutionReport(currentOrder, 0, BigDecimal.ZERO, "Modify rejected by Risk Service: "
                    + String.join("; ", riskResponse.reasons()));
            throw new InvalidOrderStateException("Modify rejected by Risk Service: "
                    + String.join("; ", riskResponse.reasons()));
        }

        OrderResponse exchangeResponse = exchangeClient.modifyOrder(orderId, request);
        OrderEntity savedOrder = upsertOrder(exchangeResponse);
        addExecutionReport(savedOrder, 0, BigDecimal.ZERO, "Order modified by OMS");
        syncExchangeState();
        return getOrder(savedOrder.getOrderId());
    }

    @Transactional
    public CancelOrderResponse cancelOrder(String orderId) {
        OrderEntity order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.PENDING_RISK) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            addExecutionReport(order, 0, BigDecimal.ZERO, "Pending risk order cancelled by OMS");
            return new CancelOrderResponse(order.getOrderId(), order.getStatus(), "Order cancelled successfully");
        }

        validateCancel(order);

        CancelOrderResponse response = exchangeClient.cancelOrder(orderId);
        order.setStatus(response.status());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        addExecutionReport(order, 0, BigDecimal.ZERO, response.message());
        return response;
    }

    public OrderResponse getOrder(String orderId) {
        return toResponse(findOrder(orderId));
    }

    public List<OrderResponse> listOrders(String symbol, String side, String status) {
        return orderRepository.findAll().stream()
                .filter(order -> symbol == null || symbol.isBlank() || order.getSymbol().equalsIgnoreCase(symbol))
                .filter(order -> side == null || side.isBlank() || order.getSide().name().equalsIgnoreCase(side))
                .filter(order -> status == null || status.isBlank() || order.getStatus().name().equalsIgnoreCase(status))
                .map(this::toResponse)
                .toList();
    }

    private void syncExchangeState() {
        tradeService.syncTradesFromExchange();
        for (TradeResponse trade : tradeService.getTrades(null)) {
            syncOrderFromExchange(trade.buyOrderId());
            syncOrderFromExchange(trade.sellOrderId());
        }
    }

    private void syncOrderFromExchange(String orderId) {
        exchangeClient.getOrder(orderId).ifPresent(this::upsertOrder);
    }

    private OrderEntity upsertOrder(OrderResponse response) {
        OrderEntity entity = orderRepository.findByOrderId(response.orderId()).orElseGet(OrderEntity::new);
        entity.setOrderId(response.orderId());
        entity.setAccountId(response.accountId());
        entity.setSymbol(response.symbol());
        entity.setSide(response.side());
        entity.setType(response.type());
        entity.setOriginalQuantity(response.originalQuantity());
        entity.setRemainingQuantity(response.remainingQuantity());
        entity.setPrice(response.price());
        entity.setStatus(response.status());
        entity.setRejectionReasons(String.join("; ", response.rejectionReasons()));
        entity.setCreatedAt(entity.getCreatedAt() == null ? response.createdAt() : entity.getCreatedAt());
        entity.setUpdatedAt(response.updatedAt());
        return orderRepository.save(entity);
    }

    private OrderEntity findOrder(String orderId) {
        return orderRepository.findByOrderId(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private OrderResponse toResponse(OrderEntity entity) {
        return toResponse(entity, defaultMessage(entity));
    }

    private OrderResponse toResponse(OrderEntity entity, String message) {
        return new OrderResponse(
                entity.getOrderId(),
                entity.getAccountId(),
                entity.getSymbol(),
                entity.getSide(),
                entity.getType(),
                entity.getOriginalQuantity(),
                entity.getRemainingQuantity(),
                entity.getPrice(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                parseRejectionReasons(entity.getRejectionReasons()),
                message
        );
    }

    private OrderEntity savePendingRiskOrder(CreateOrderRequest request) {
        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = new OrderEntity();
        order.setOrderId(nextOrderId());
        order.setAccountId(request.accountId());
        order.setSymbol(request.symbol());
        order.setSide(request.side());
        order.setType(request.type());
        order.setOriginalQuantity(request.quantity());
        order.setRemainingQuantity(request.quantity());
        order.setPrice(request.type() == OrderType.MARKET ? BigDecimal.ZERO : request.price());
        order.setStatus(OrderStatus.PENDING_RISK);
        order.setRejectionReasons("");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return orderRepository.save(order);
    }

    @Transactional
    public void handleRiskApproved(OrderRiskApprovedEvent event) {
        OrderEntity order = findOrder(event.orderId());
        if (order.getStatus() != OrderStatus.PENDING_RISK) {
            return;
        }

        order.setStatus(OrderStatus.RISK_APPROVED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        addExecutionReport(order, 0, BigDecimal.ZERO, event.message());

        OrderResponse exchangeResponse = exchangeClient.placeOrder(order.getOrderId(), toCreateRequest(order));
        OrderEntity savedOrder = upsertOrder(exchangeResponse);
        addExecutionReport(savedOrder, 0, BigDecimal.ZERO, "Order routed to exchange after risk approval");
        syncExchangeState();
    }

    @Transactional
    public void handleRiskRejected(OrderRiskRejectedEvent event) {
        OrderEntity order = findOrder(event.orderId());
        if (order.getStatus() == OrderStatus.REJECTED) {
            return;
        }

        order.setStatus(OrderStatus.REJECTED);
        order.setRejectionReasons(String.join("; ", event.reasons()));
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        addExecutionReport(order, 0, BigDecimal.ZERO, "Order rejected by Risk Service: "
                + String.join("; ", event.reasons()));
    }

    private String nextOrderId() {
        return "ORD-" + nextOrderSequence.getAndIncrement();
    }

    private CreateOrderRequest toCreateRequest(OrderEntity order) {
        return new CreateOrderRequest(
                order.getAccountId(),
                order.getSymbol(),
                order.getSide(),
                order.getType(),
                order.getOriginalQuantity(),
                order.getType() == OrderType.MARKET ? null : order.getPrice()
        );
    }

    private OrderSubmittedEvent toOrderSubmittedEvent(OrderEntity order) {
        return new OrderSubmittedEvent(
                UUID.randomUUID().toString(),
                order.getOrderId(),
                order.getAccountId(),
                order.getSymbol(),
                order.getSide().name(),
                order.getType().name(),
                order.getOriginalQuantity(),
                order.getType() == OrderType.MARKET ? null : order.getPrice(),
                order.getCreatedAt()
        );
    }

    private CreateOrderRequest normalizeCreateRequest(CreateOrderRequest request) {
        return new CreateOrderRequest(
                normalizeAccountId(request.accountId()),
                request.symbol().toUpperCase(),
                request.side(),
                request.type(),
                request.quantity(),
                request.price()
        );
    }

    private void validateCreateOrder(CreateOrderRequest request) {
        if (request.symbol() == null || request.symbol().isBlank()) {
            throw new InvalidOrderException("Symbol is required");
        }

        if (request.side() == null) {
            throw new InvalidOrderException("Side is required");
        }

        if (request.type() == null) {
            throw new InvalidOrderException("Type is required");
        }

        if (request.quantity() <= 0) {
            throw new InvalidOrderException("Quantity must be greater than zero");
        }

        if (request.type() == OrderType.LIMIT) {
            validateLimitPrice(request.price());
        }

        if (request.type() == OrderType.MARKET
                && request.price() != null
                && request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOrderException("Market order price cannot be negative");
        }
    }

    private void validateModify(OrderEntity order, ModifyOrderRequest request) {
        if (!isActive(order)) {
            throw new InvalidOrderStateException("Only active orders can be modified");
        }

        if (order.getType() != OrderType.LIMIT) {
            throw new InvalidOrderStateException("Only active LIMIT orders can be modified");
        }

        if (request.quantity() <= 0) {
            throw new InvalidOrderException("Quantity must be greater than zero");
        }

        validateLimitPrice(request.price());
    }

    private void validateCancel(OrderEntity order) {
        if (!isActive(order)) {
            throw new InvalidOrderStateException("Only active orders can be cancelled");
        }
    }

    private boolean isActive(OrderEntity order) {
        return order.getType() == OrderType.LIMIT
                && (order.getStatus() == OrderStatus.NEW || order.getStatus() == OrderStatus.PARTIALLY_FILLED);
    }

    private void validateLimitPrice(BigDecimal price) {
        if (price == null) {
            throw new InvalidOrderException("LIMIT order requires price");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("LIMIT price must be greater than zero");
        }
    }

    private void addExecutionReport(
            OrderEntity order,
            int executedQuantity,
            BigDecimal executedPrice,
            String message
    ) {
        ExecutionReportEntity report = new ExecutionReportEntity();
        report.setExecutionId("EXE-" + nextExecutionSequence.getAndIncrement());
        report.setOrderId(order.getOrderId());
        report.setSymbol(order.getSymbol());
        report.setSide(order.getSide());
        report.setStatus(order.getStatus());
        report.setExecutedQuantity(executedQuantity);
        report.setExecutedPrice(executedPrice);
        report.setRemainingQuantity(order.getRemainingQuantity());
        report.setMessage(message);
        report.setCreatedAt(LocalDateTime.now());
        executionReportRepository.save(report);
    }

    private String normalizeAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return "TRADER-1";
        }
        return accountId.toUpperCase();
    }

    private List<String> parseRejectionReasons(String rejectionReasons) {
        if (rejectionReasons == null || rejectionReasons.isBlank()) {
            return List.of();
        }
        return Arrays.asList(rejectionReasons.split("; "));
    }

    private String defaultMessage(OrderEntity order) {
        if (order.getStatus() == OrderStatus.PENDING_RISK) {
            return "Order submitted for risk evaluation";
        }
        if (order.getStatus() == OrderStatus.REJECTED) {
            return "Order rejected";
        }
        return "Order status: " + order.getStatus();
    }
}
