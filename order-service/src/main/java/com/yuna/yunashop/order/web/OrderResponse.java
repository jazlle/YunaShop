package com.yuna.yunashop.order.web;

import com.yuna.yunashop.order.domain.Order;
import com.yuna.yunashop.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(UUID id, String customerId, String productId, int quantity, OrderStatus status,
                            BigDecimal totalAmount, String failureReason, Instant createdAt, Instant updatedAt) {

    static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getProductId(), order.getQuantity(),
                order.getStatus(), order.getTotalAmount(), order.getFailureReason(), order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
