package com.yuna.yunashop.order.domain;

import com.yuna.yunashop.events.OrderCancelledEvent;
import com.yuna.yunashop.events.OrderCreatedEvent;
import com.yuna.yunashop.order.messaging.Topics;
import com.yuna.yunashop.order.outbox.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Outbox outbox;

    public OrderService(OrderRepository orderRepository, Outbox outbox) {
        this.orderRepository = orderRepository;
        this.outbox = outbox;
    }

    /**
     * Enregistre la commande en PENDING et, dans la même transaction, l'événement {@code order.created}
     * qui démarre la saga. Il sera publié sur Kafka par le relais de l'outbox.
     */
    @Transactional
    public Order placeOrder(String customerId, String productId, int quantity) {
        Order order = orderRepository.save(Order.create(customerId, productId, quantity));
        OrderCreatedEvent event = OrderCreatedEvent.newBuilder()
                .setOrderId(order.getId())
                .setCustomerId(customerId)
                .setProductId(productId)
                .setQuantity(quantity)
                .build();
        outbox.append(Topics.ORDER_CREATED, order.getId(), event);
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

    /**
     * Abandonne une saga restée trop longtemps sans issue : la commande passe en CANCELLED et
     * {@code order.cancelled} demande aux autres services de compenser.
     */
    @Transactional
    public void expire(UUID orderId, String reason) {
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            // Une réponse de la saga est arrivée entre la recherche et ce traitement.
            return;
        }
        order.cancel(reason);
        if (outbox.discardPending(Topics.ORDER_CREATED, orderId) > 0) {
            // order.created n'était pas encore publié : inutile de démarrer une saga déjà abandonnée.
            log.info("Order {} expired before its saga started", orderId);
        }
        // Publié même dans ce cas : un envoi en échec côté relais (timeout) a pu atteindre Kafka malgré tout.
        // Les participants qui n'ont rien fait pour cette commande n'ont rien à compenser.
        outbox.append(Topics.ORDER_CANCELLED, orderId, OrderCancelledEvent.newBuilder()
                .setOrderId(orderId)
                .setReason(reason)
                .build());
        log.info("Order {} is now {} ({})", orderId, order.getStatus(), reason);
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
