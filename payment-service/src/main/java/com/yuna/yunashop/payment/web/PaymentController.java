package com.yuna.yunashop.payment.web;

import com.yuna.yunashop.payment.domain.CustomerAccount;
import com.yuna.yunashop.payment.domain.Payment;
import com.yuna.yunashop.payment.domain.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
class PaymentController {

    record PaymentResponse(UUID id, UUID orderId, String customerId, BigDecimal amount, Payment.Status status,
                           String failureReason, Instant createdAt) {

        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getCustomerId(),
                    payment.getAmount(), payment.getStatus(), payment.getFailureReason(), payment.getCreatedAt());
        }
    }

    record AccountResponse(String customerId, BigDecimal balance) {

        static AccountResponse from(CustomerAccount account) {
            return new AccountResponse(account.getCustomerId(), account.getBalance());
        }
    }

    private final PaymentService paymentService;

    PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payments")
    List<PaymentResponse> getPayments() {
        return paymentService.getPayments().stream().map(PaymentResponse::from).toList();
    }

    @GetMapping("/payments/order/{orderId}")
    ResponseEntity<PaymentResponse> getPaymentForOrder(@PathVariable UUID orderId) {
        return ResponseEntity.of(paymentService.getPaymentForOrder(orderId).map(PaymentResponse::from));
    }

    @GetMapping("/accounts/{customerId}")
    ResponseEntity<AccountResponse> getAccount(@PathVariable String customerId) {
        return ResponseEntity.of(paymentService.getAccount(customerId).map(AccountResponse::from));
    }
}
