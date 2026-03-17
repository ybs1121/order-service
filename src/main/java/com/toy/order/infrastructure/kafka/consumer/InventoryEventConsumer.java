package com.toy.order.infrastructure.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toy.order.application.service.OrderService;
import com.toy.order.infrastructure.kafka.event.InventoryDeductedEvent;
import com.toy.order.infrastructure.kafka.event.InventoryInsufficientEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.deducted", groupId = "order-consumer-group")
    public void handleInventoryDeducted(String message) {
        InventoryDeductedEvent event = fromJson(message, InventoryDeductedEvent.class);
        log.info("Consumed InventoryDeductedEvent: orderId={}", event.orderId());
        orderService.handleInventoryDeducted(event);
    }

    @KafkaListener(topics = "inventory.insufficient", groupId = "order-consumer-group")
    public void handleInventoryInsufficient(String message) {
        InventoryInsufficientEvent event = fromJson(message, InventoryInsufficientEvent.class);
        log.warn("Consumed InventoryInsufficientEvent: orderId={}", event.orderId());
        orderService.handleInventoryInsufficient(event);
    }

    private <T> T fromJson(String message, Class<T> type) {
        try {
            return objectMapper.readValue(message, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize kafka event", e);
        }
    }
}
