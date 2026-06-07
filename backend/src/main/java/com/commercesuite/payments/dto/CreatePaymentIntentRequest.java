package com.commercesuite.payments.dto;
import com.commercesuite.payments.entity.PaymentMethodKind;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreatePaymentIntentRequest(
    @NotNull UUID checkoutId,
    UUID orderId,
    @NotNull @Min(1) Long amountPaise,
    @NotNull PaymentMethodKind methodKind,
    UUID paymentMethodId,
    @Size(max=64) String gatewayProvider
) {}