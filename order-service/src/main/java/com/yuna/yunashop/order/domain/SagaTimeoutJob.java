package com.yuna.yunashop.order.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Compensation déclenchée par le temps : une commande sans issue après {@code yunashop.saga.timeout}
 * est annulée, quel que soit l'endroit où la saga s'est arrêtée (service en panne, message perdu...).
 */
@Component
class SagaTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(SagaTimeoutJob.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final Duration timeout;
    private final int batchSize;

    SagaTimeoutJob(OrderRepository orderRepository, OrderService orderService,
                   @Value("${yunashop.saga.timeout}") Duration timeout,
                   @Value("${yunashop.saga.timeout-batch-size}") int batchSize) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.timeout = timeout;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${yunashop.saga.timeout-check-interval}")
    void expireStalePendingOrders() {
        Instant deadline = Instant.now().minus(timeout);
        String reason = "Saga timed out after " + timeout.toSeconds() + "s";
        for (Order order : orderRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAt(
                OrderStatus.PENDING, deadline, Limit.of(batchSize))) {
            try {
                // Une transaction par commande : un échec n'annule pas le reste du lot.
                orderService.expire(order.getId(), reason);
            } catch (OptimisticLockingFailureException e) {
                // Un événement de la saga a modifié la commande en même temps : il a gagné.
                log.info("Order {} changed while expiring it, skipped", order.getId());
            }
        }
    }
}
