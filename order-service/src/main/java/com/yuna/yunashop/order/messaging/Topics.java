package com.yuna.yunashop.order.messaging;

/** Topics Kafka utilisés par order-service (contrat dupliqué volontairement dans chaque service). */
public final class Topics {

    public static final String ORDER_CREATED = "order.created";
    public static final String INVENTORY_FAILED = "inventory.failed";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";

    private Topics() {
    }
}
