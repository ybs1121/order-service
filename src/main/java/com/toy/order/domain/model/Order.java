package com.toy.order.domain.model;

public class Order {

    private final OrderId id;
    private final ProductId productId;
    private final int quantity;
    private final OrderStatus status;

    private Order(OrderId id, ProductId productId, int quantity, OrderStatus status) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
    }

    public static Order create(OrderId id, ProductId productId, int quantity) {
        return new Order(id, productId, quantity, OrderStatus.PENDING);
    }

    public static Order of(OrderId id, ProductId productId, int quantity, OrderStatus status) {
        return new Order(id, productId, quantity, status);
    }

    public Order confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm order in status: " + this.status);
        }
        return new Order(this.id, this.productId, this.quantity, OrderStatus.CONFIRMED);
    }

    public Order cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }
        return new Order(this.id, this.productId, this.quantity, OrderStatus.CANCELLED);
    }

    public boolean isCancellable() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.CONFIRMED;
    }

    public OrderId getId() {
        return id;
    }

    public ProductId getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
