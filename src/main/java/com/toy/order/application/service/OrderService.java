package com.toy.order.application.service;

import com.toy.order.domain.model.Order;
import com.toy.order.domain.model.OrderId;
import com.toy.order.domain.model.ProductId;
import com.toy.order.domain.repository.OrderRepository;
import com.toy.order.infrastructure.idempotency.ProcessedEvent;
import com.toy.order.infrastructure.idempotency.ProcessedEventRepository;
import com.toy.order.infrastructure.kafka.event.InventoryDeductedEvent;
import com.toy.order.infrastructure.kafka.event.InventoryInsufficientEvent;
import com.toy.order.infrastructure.kafka.event.OrderCancelledEvent;
import com.toy.order.infrastructure.kafka.event.OrderCreatedEvent;
import com.toy.order.infrastructure.outbox.OutboxEventSaver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventSaver outboxEventSaver;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public Order placeOrder(String productId, int quantity) {
        OrderId orderId = new OrderId(UUID.randomUUID().toString());
        Order order = Order.create(orderId, new ProductId(productId), quantity);
        Order saved = orderRepository.save(order);

        outboxEventSaver.save("order.created", orderId.value(), new OrderCreatedEvent(
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

        outboxEventSaver.save("order.cancelled", orderId, new OrderCancelledEvent(
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

    /**
     * 멱등성: processed_events INSERT(eventId 유니크)를 비즈니스 처리 전에 시도.
     * 같은 eventId가 다시 오면 유니크 위반 → skip.
     * 비즈니스 처리 실패 시 트랜잭션 롤백 → processed_events도 롤백 → 재처리 가능.
     */
    @Transactional
    public void handleInventoryDeducted(InventoryDeductedEvent event, String eventId) {
        if (!markProcessed(eventId, "inventory.deducted")) return;

        log.info("Handling InventoryDeductedEvent: orderId={}", event.orderId());
        orderRepository.findById(new OrderId(event.orderId()))
                .ifPresent(order -> {
                    Order confirmed = order.confirm();
                    orderRepository.save(confirmed);
                    log.info("Order confirmed: orderId={}", event.orderId());
                });
    }

    @Transactional
    public void handleInventoryInsufficient(InventoryInsufficientEvent event, String eventId) {
        if (!markProcessed(eventId, "inventory.insufficient")) return;

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

    /**
     * eventId 기반으로 중복 여부를 판단한다.
     * INSERT 성공 → 처음 처리되는 이벤트 (true 반환)
     * 유니크 위반 → 이미 처리된 이벤트 (false 반환)
     */
    private boolean markProcessed(String eventId, String topic) {
        try {
            processedEventRepository.saveAndFlush(new ProcessedEvent(eventId, topic));
            return true;
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate event detected, skipping. eventId={}, topic={}", eventId, topic);
            return false;
        }
    }
}
