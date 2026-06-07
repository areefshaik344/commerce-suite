package com.commercesuite.checkout.dto;

import com.commercesuite.checkout.entity.PaymentMethodKind;
import jakarta.validation.constraints.NotNull;

public record SelectPaymentRequest(@NotNull PaymentMethodKind method, String couponCode) {}