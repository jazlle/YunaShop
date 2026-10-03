package com.yuna.yunashop.order.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(@NotBlank String customerId, @NotBlank String productId, @Positive int quantity) {
}
