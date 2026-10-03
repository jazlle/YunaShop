package com.yuna.yunashop.order.messaging;

import com.yuna.yunashop.events.InventoryFailedEvent;
import com.yuna.yunashop.events.PaymentCompletedEvent;
import com.yuna.yunashop.events.PaymentFailedEvent;
import com.yuna.yunashop.order.domain.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Fait évoluer le statut de la commande selon l'issue de la saga. */
@Component
class OrderSagaListener {

    private final OrderService orderService;

    OrderSagaListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED)
    void onPaymentCompleted(PaymentCompletedEvent event) {
        orderService.confirm(event.getOrderId(), event.getAmount());
    }

    @KafkaListener(topics = Topics.INVENTORY_FAILED)
    void onInventoryFailed(InventoryFailedEvent event) {
        orderService.reject(event.getOrderId(), event.getReason());
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    void onPaymentFailed(PaymentFailedEvent event) {
        orderService.cancel(event.getOrderId(), event.getReason());
    }
}
