package com.toy.order.infrastructure.persistence;

import com.toy.order.domain.model.Order;
import com.toy.order.domain.model.OrderId;
import com.toy.order.domain.model.ProductId;
import com.toy.order.domain.repository.OrderRepository;
import com.toy.order.infrastructure.persistence.entity.OrderEntity;
import com.toy.order.infrastructure.persistence.jpa.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    @Override
    public Order save(Order order) {
        OrderEntity entity = jpaRepository.findById(order.getId().value())
                .orElse(new OrderEntity(
                        order.getId().value(),
                        order.getProductId().value(),
                        order.getQuantity(),
                        order.getStatus()
                ));
        entity.updateStatus(order.getStatus());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.value())
                .map(this::toDomain);
    }

    private Order toDomain(OrderEntity entity) {
        return Order.of(
                new OrderId(entity.getId()),
                new ProductId(entity.getProductId()),
                entity.getQuantity(),
                entity.getStatus()
        );
    }
}
