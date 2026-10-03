package com.yuna.yunashop.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    public enum Status { COMPLETED, FAILED }

    @Id
    private UUID id;

    /** Un seul paiement par commande : garantit l'idempotence en cas de message rejoué. */
    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    private Payment(UUID orderId, String customerId, BigDecimal amount, Status status, String failureReason) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = Instant.now();
    }

    public static Payment completed(UUID orderId, String customerId, BigDecimal amount) {
        return new Payment(orderId, customerId, amount, Status.COMPLETED, null);
    }

    public static Payment failed(UUID orderId, String customerId, BigDecimal amount, String reason) {
        return new Payment(orderId, customerId, amount, Status.FAILED, reason);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Status getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
