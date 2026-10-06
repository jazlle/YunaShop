package com.yuna.yunashop.order.domain;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByStatusAndCreatedAtBeforeOrderByCreatedAt(OrderStatus status, Instant createdBefore, Limit limit);
}
