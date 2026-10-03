package com.yuna.yunashop.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_accounts")
public class CustomerAccount {

    @Id
    private String customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @Version
    private Long version;

    protected CustomerAccount() {
    }

    public boolean canPay(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    public void debit(BigDecimal amount) {
        if (!canPay(amount)) {
            throw new IllegalStateException("Insufficient funds for customer " + customerId);
        }
        balance = balance.subtract(amount);
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
