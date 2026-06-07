package com.commercesuite.payments.dto;
import com.commercesuite.payments.entity.PaymentAttempt;
import com.commercesuite.payments.entity.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentAttemptDto(UUID id, UUID intentId, int attemptNumber, PaymentStatus status,
                                String gatewayProvider, String gatewayReference,
                                String failureCode, String failureMessage, Instant attemptedAt) {
  public static PaymentAttemptDto from(PaymentAttempt a) {
    return new PaymentAttemptDto(a.getId(), a.getIntentId(), a.getAttemptNumber(), a.getStatus(),
        a.getGatewayProvider(), a.getGatewayReference(),
        a.getFailureCode(), a.getFailureMessage(), a.getAttemptedAt());
  }
}