package com.marketx.oms.controller;

import com.marketx.oms.dto.OrderBookResponse;
import com.marketx.oms.service.OrderBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order-book")
public class OrderBookController {
    private final OrderBookService orderBookService;

    public OrderBookController(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<OrderBookResponse> getOrderBook(@PathVariable String symbol) {
        return ResponseEntity.ok(orderBookService.getOrderBook(symbol));
    }
}
