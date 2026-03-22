package com.toy.order.infrastructure.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toy.order.application.service.OrderService;
import com.toy.order.infrastructure.kafka.event.InventoryDeductedEvent;
import com.toy.order.infrastructure.kafka.event.InventoryInsufficientEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.deducted", groupId = "order-consumer-group")
    public void handleInventoryDeducted(
            @Payload String message,
            @Header("X-Event-Id") String eventId) {
        InventoryDeductedEvent event = fromJson(message, InventoryDeductedEvent.class);
        log.info("Consumed InventoryDeductedEvent: orderId={}, eventId={}", event.orderId(), eventId);
        orderService.handleInventoryDeducted(event, eventId);
    }

    @KafkaListener(topics = "inventory.insufficient", groupId = "order-consumer-group")
    public void handleInventoryInsufficient(
            @Payload String message,
            @Header("X-Event-Id") String eventId) {
        InventoryInsufficientEvent event = fromJson(message, InventoryInsufficientEvent.class);
        log.warn("Consumed InventoryInsufficientEvent: orderId={}, eventId={}", event.orderId(), eventId);
        orderService.handleInventoryInsufficient(event, eventId);
    }

    private <T> T fromJson(String message, Class<T> type) {
        try {
            return objectMapper.readValue(message, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize kafka event", e);
        }
    }
}
