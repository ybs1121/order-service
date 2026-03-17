package com.toy.order.application.service;

import com.toy.order.domain.model.Order;
import com.toy.order.domain.model.OrderId;
import com.toy.order.domain.model.ProductId;
import com.toy.order.domain.repository.OrderRepository;
import com.toy.order.infrastructure.kafka.event.InventoryDeductedEvent;
import com.toy.order.infrastructure.kafka.event.InventoryInsufficientEvent;
import com.toy.order.infrastructure.kafka.event.OrderCancelledEvent;
import com.toy.order.infrastructure.kafka.event.OrderCreatedEvent;
import com.toy.order.infrastructure.kafka.producer.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;

    @Transactional
    public Order placeOrder(String productId, int quantity) {
        OrderId orderId = new OrderId(UUID.randomUUID().toString());
        Order order = Order.create(orderId, new ProductId(productId), quantity);
        Order saved = orderRepository.save(order);

        eventProducer.publishOrderCreated(new OrderCreatedEvent(
                orderId.value(),
                productId,
                quantity,
                LocalDateTime.now()
        ));

        log.info("Order placed: orderId={}, productId={}, quantity={}", orderId.value(), productId, quantity);
        return saved;
    }

    @Transactional
    public Order cancelOrder(String orderId) {
        Order order = orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.isCancellable()) {
            throw new IllegalStateException("Order cannot be cancelled in status: " + order.getStatus());
        }

        Order cancelled = order.cancel();
        Order saved = orderRepository.save(cancelled);

        eventProducer.publishOrderCancelled(new OrderCancelledEvent(
                orderId,
                order.getProductId().value(),
                order.getQuantity(),
                LocalDateTime.now()
        ));

        log.info("Order cancelled: orderId={}", orderId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Order findOrder(String orderId) {
        return orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Transactional
    public void handleInventoryDeducted(InventoryDeductedEvent event) {
        log.info("Handling InventoryDeductedEvent: orderId={}", event.orderId());
        orderRepository.findById(new OrderId(event.orderId()))
                .ifPresent(order -> {
                    Order confirmed = order.confirm();
                    orderRepository.save(confirmed);
                    log.info("Order confirmed: orderId={}", event.orderId());
                });
    }

    @Transactional
    public void handleInventoryInsufficient(InventoryInsufficientEvent event) {
        log.warn("Handling InventoryInsufficientEvent: orderId={}, available={}", event.orderId(), event.availableStock());
        orderRepository.findById(new OrderId(event.orderId()))
                .ifPresent(order -> {
                    if (order.isCancellable()) {
                        Order cancelled = order.cancel();
                        orderRepository.save(cancelled);
                        log.warn("Order auto-cancelled due to insufficient stock: orderId={}", event.orderId());
                    }
                });
    }
}
