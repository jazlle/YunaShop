package com.yuna.yunashop.payment.messaging;

import com.yuna.yunashop.events.InventoryReservedEvent;
import com.yuna.yunashop.events.PaymentCompletedEvent;
import com.yuna.yunashop.events.PaymentFailedEvent;
import com.yuna.yunashop.payment.domain.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class PaymentSagaListener {

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    PaymentSagaListener(PaymentService paymentService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Publie le résultat seulement après le commit de la transaction de paiement. */
    @KafkaListener(topics = Topics.INVENTORY_RESERVED)
    void onInventoryReserved(InventoryReservedEvent event) {
        String key = event.getOrderId().toString();
        switch (paymentService.pay(event)) {
            case PaymentCompletedEvent completed -> kafkaTemplate.send(Topics.PAYMENT_COMPLETED, key, completed);
            case PaymentFailedEvent failed -> kafkaTemplate.send(Topics.PAYMENT_FAILED, key, failed);
            default -> throw new IllegalStateException("Unexpected payment outcome");
        }
    }
}
