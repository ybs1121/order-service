package com.toy.order.infrastructure.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toy.order.infrastructure.kafka.event.OrderCancelledEvent;
import com.toy.order.infrastructure.kafka.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final String TOPIC_CREATED = "order.created";
    private static final String TOPIC_CANCELLED = "order.cancelled";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(TOPIC_CREATED, event.orderId(), toJson(event));
        log.info("Published OrderCreatedEvent: orderId={}", event.orderId());
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        kafkaTemplate.send(TOPIC_CANCELLED, event.orderId(), toJson(event));
        log.info("Published OrderCancelledEvent: orderId={}", event.orderId());
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize kafka event", e);
        }
    }
}
