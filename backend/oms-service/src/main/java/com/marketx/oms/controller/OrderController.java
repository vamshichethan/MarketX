package com.marketx.oms.controller;

import com.marketx.oms.dto.CancelOrderResponse;
import com.marketx.oms.dto.CreateOrderRequest;
import com.marketx.oms.dto.ModifyOrderRequest;
import com.marketx.oms.dto.OrderResponse;
import com.marketx.oms.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<OrderResponse> modifyOrder(
            @PathVariable String orderId,
            @RequestBody ModifyOrderRequest request
    ) {
        return ResponseEntity.ok(orderService.modifyOrder(orderId, request));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<CancelOrderResponse> cancelOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String side,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(orderService.listOrders(symbol, side, status));
    }
}
