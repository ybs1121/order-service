package com.toy.order.infrastructure.kafka.event;

import java.time.LocalDateTime;

public record InventoryDeductedEvent(
        String orderId,
        String productId,
        int deductedQuantity,
        LocalDateTime occurredAt
) {}
