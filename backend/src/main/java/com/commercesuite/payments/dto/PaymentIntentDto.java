package com.commercesuite.payments.dto;
import com.commercesuite.payments.entity.PaymentIntent;
import com.commercesuite.payments.entity.PaymentMethodKind;
import com.commercesuite.payments.entity.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentIntentDto(
    UUID id, UUID customerId, UUID orderId, UUID checkoutId,
    PaymentStatus status, String currency,
    long amountPaise, long authorizedPaise, long capturedPaise, long refundedPaise,
    long refundableRemainingPaise,
    PaymentMethodKind methodKind, UUID paymentMethodId,
    String gatewayProvider, String gatewayIntentId,
    String failureCode, String failureMessage,
    Instant authorizedAt, Instant capturedAt, Instant failedAt, Instant cancelledAt,
    Instant createdAt
) {
  public static PaymentIntentDto from(PaymentIntent p) {
    return new PaymentIntentDto(p.getId(), p.getCustomerId(), p.getOrderId(), p.getCheckoutId(),
        p.getStatus(), p.getCurrency(), p.getAmountPaise(), p.getAuthorizedPaise(),
        p.getCapturedPaise(), p.getRefundedPaise(), p.refundableRemainingPaise(),
        p.getMethodKind(), p.getPaymentMethodId(),
        p.getGatewayProvider(), p.getGatewayIntentId(),
        p.getFailureCode(), p.getFailureMessage(),
        p.getAuthorizedAt(), p.getCapturedAt(), p.getFailedAt(), p.getCancelledAt(),
        p.getCreatedAt());
  }
}