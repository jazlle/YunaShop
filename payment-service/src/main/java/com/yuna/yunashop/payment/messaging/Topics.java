package com.yuna.yunashop.payment.messaging;

/** Topics Kafka utilisés par payment-service (contrat dupliqué volontairement dans chaque service). */
public final class Topics {

    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";

    private Topics() {
    }
}
