package com.toy.order.infrastructure.kafka.event;

import java.time.LocalDateTime;

public record InventoryInsufficientEvent(
        String orderId,
        String productId,
        int requestedQuantity,
        int availableStock,
        LocalDateTime occurredAt
) {}
