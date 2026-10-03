package com.yuna.yunashop.inventory.messaging;

import com.yuna.yunashop.events.InventoryFailedEvent;
import com.yuna.yunashop.events.InventoryReservedEvent;
import com.yuna.yunashop.events.OrderCreatedEvent;
import com.yuna.yunashop.events.PaymentFailedEvent;
import com.yuna.yunashop.inventory.domain.InventoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class InventorySagaListener {

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    InventorySagaListener(InventoryService inventoryService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Publie le résultat seulement après le commit de la transaction de réservation. */
    @KafkaListener(topics = Topics.ORDER_CREATED)
    void onOrderCreated(OrderCreatedEvent event) {
        String key = event.getOrderId().toString();
        switch (inventoryService.reserve(event)) {
            case InventoryReservedEvent reserved -> kafkaTemplate.send(Topics.INVENTORY_RESERVED, key, reserved);
            case InventoryFailedEvent failed -> kafkaTemplate.send(Topics.INVENTORY_FAILED, key, failed);
            default -> throw new IllegalStateException("Unexpected reservation outcome");
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    void onPaymentFailed(PaymentFailedEvent event) {
        inventoryService.release(event.getOrderId());
    }
}
