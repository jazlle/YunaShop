package com.yuna.yunashop.payment.domain;

import com.yuna.yunashop.events.InventoryReservedEvent;
import com.yuna.yunashop.events.PaymentCompletedEvent;
import com.yuna.yunashop.events.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final CustomerAccountRepository accountRepository;

    public PaymentService(PaymentRepository paymentRepository, CustomerAccountRepository accountRepository) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Débite le compte du client du montant de la commande.
     *
     * @return un {@link PaymentCompletedEvent} ou un {@link PaymentFailedEvent} à publier
     */
    @Transactional
    public Object pay(InventoryReservedEvent reservation) {
        Optional<Payment> existing = paymentRepository.findByOrderId(reservation.getOrderId());
        if (existing.isPresent()) {
            // Message rejoué : on republie le résultat déjà enregistré sans débiter une seconde fois.
            log.warn("Order {} already paid, replaying outcome", reservation.getOrderId());
            return toEvent(existing.get());
        }

        Optional<CustomerAccount> account = accountRepository.findById(reservation.getCustomerId());
        Payment payment;
        if (account.isEmpty()) {
            payment = Payment.failed(reservation.getOrderId(), reservation.getCustomerId(), reservation.getTotalAmount(),
                    "Unknown customer " + reservation.getCustomerId());
        } else if (!account.get().canPay(reservation.getTotalAmount())) {
            payment = Payment.failed(reservation.getOrderId(), reservation.getCustomerId(), reservation.getTotalAmount(),
                    "Insufficient funds");
        } else {
            account.get().debit(reservation.getTotalAmount());
            payment = Payment.completed(reservation.getOrderId(), reservation.getCustomerId(), reservation.getTotalAmount());
        }

        paymentRepository.save(payment);
        log.info("Payment for order {}: {}", reservation.getOrderId(), payment.getStatus());
        return toEvent(payment);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPayments() {
        return paymentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentForOrder(UUID orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Optional<CustomerAccount> getAccount(String customerId) {
        return accountRepository.findById(customerId);
    }

    private static Object toEvent(Payment payment) {
        return switch (payment.getStatus()) {
            case COMPLETED -> PaymentCompletedEvent.newBuilder()
                    .setOrderId(payment.getOrderId())
                    .setPaymentId(payment.getId())
                    // Le type Avro decimal(12, 2) impose une échelle exacte de 2
                    .setAmount(payment.getAmount().setScale(2, RoundingMode.HALF_UP))
                    .build();
            case FAILED -> PaymentFailedEvent.newBuilder()
                    .setOrderId(payment.getOrderId())
                    .setReason(payment.getFailureReason())
                    .build();
        };
    }
}
