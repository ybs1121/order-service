package com.toy.order.infrastructure.kafka.event;

import java.time.LocalDateTime;

public record OrderCreatedEvent(
        String orderId,
        String productId,
        int quantity,
        LocalDateTime occurredAt
) {}
