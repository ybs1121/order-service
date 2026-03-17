package com.toy.order.presentation.controller.dto;

import com.toy.order.domain.model.Order;
import com.toy.order.domain.model.OrderStatus;

public record OrderResponse(
        String orderId,
        String productId,
        int quantity,
        OrderStatus status
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId().value(),
                order.getProductId().value(),
                order.getQuantity(),
                order.getStatus()
        );
    }
}
