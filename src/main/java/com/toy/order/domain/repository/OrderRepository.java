package com.toy.order.domain.repository;

import com.toy.order.domain.model.Order;
import com.toy.order.domain.model.OrderId;

import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId orderId);
}
