package com.toy.order.presentation.controller;

import com.toy.order.application.service.OrderService;
import com.toy.order.domain.model.Order;
import com.toy.order.presentation.controller.dto.OrderPlaceRequest;
import com.toy.order.presentation.controller.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderPlaceRequest request) {
        Order order = orderService.placeOrder(request.productId(), request.quantity());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        Order order = orderService.findOrder(orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId) {
        Order order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
