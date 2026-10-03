package com.yuna.yunashop.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Long version;

    protected Order() {
    }

    public static Order create(String customerId, String productId, int quantity) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.productId = productId;
        order.quantity = quantity;
        order.status = OrderStatus.PENDING;
        order.createdAt = Instant.now();
        return order;
    }

    public void confirm(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        transitionTo(OrderStatus.CONFIRMED, null);
    }

    public void reject(String reason) {
        transitionTo(OrderStatus.REJECTED, reason);
    }

    public void cancel(String reason) {
        transitionTo(OrderStatus.CANCELLED, reason);
    }

    private void transitionTo(OrderStatus target, String reason) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order " + id + " is already " + status);
        }
        this.status = target;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
