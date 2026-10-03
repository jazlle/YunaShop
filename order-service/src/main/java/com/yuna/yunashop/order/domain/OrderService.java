package com.yuna.yunashop.order.domain;

import com.yuna.yunashop.events.OrderCreatedEvent;
import com.yuna.yunashop.order.messaging.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderService(OrderRepository orderRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Enregistre la commande en PENDING puis publie {@code order.created} pour démarrer la saga.
     * <p>
     * NB : l'écriture en base et l'envoi Kafka ne sont pas atomiques. Pour une garantie forte,
     * utiliser le pattern Transactional Outbox.
     */
    public Order placeOrder(String customerId, String productId, int quantity) {
        Order order = orderRepository.save(Order.create(customerId, productId, quantity));
        OrderCreatedEvent event = OrderCreatedEvent.newBuilder()
                .setOrderId(order.getId())
                .setCustomerId(customerId)
                .setProductId(productId)
                .setQuantity(quantity)
                .build();
        kafkaTemplate.send(Topics.ORDER_CREATED, order.getId().toString(), event);
        log.info("Order {} placed, saga started", order.getId());
        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public void confirm(UUID orderId, BigDecimal totalAmount) {
        update(orderId, order -> order.confirm(totalAmount));
    }

    @Transactional
    public void reject(UUID orderId, String reason) {
        update(orderId, order -> order.reject(reason));
    }

    @Transactional
    public void cancel(UUID orderId, String reason) {
        update(orderId, order -> order.cancel(reason));
    }

    private void update(UUID orderId, Consumer<Order> transition) {
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            // Kafka livre "at-least-once" : un message rejoué trouve la commande déjà dans un état final.
            log.warn("Ignoring event for order {} already {}", orderId, order.getStatus());
            return;
        }
        transition.accept(order);
        log.info("Order {} is now {}", orderId, order.getStatus());
    }
}
