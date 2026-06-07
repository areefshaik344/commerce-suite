package com.commercesuite.payments.event;
import com.commercesuite.payments.entity.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public final class PaymentEvents {
  private PaymentEvents() {}
  public record PaymentCreatedEvent(UUID intentId, UUID customerId, UUID orderId, long amountPaise, Instant at) {}
  public record PaymentAuthorizedEvent(UUID intentId, long amountPaise, Instant at) {}
  public record PaymentCapturedEvent(UUID intentId, UUID orderId, long amountPaise, Instant at) {}
  public record PaymentFailedEvent(UUID intentId, String code, String message, Instant at) {}
  public record PaymentCancelledEvent(UUID intentId, UUID actorId, String reason, Instant at) {}
  public record PaymentRefundedEvent(UUID intentId, long refundedPaise, long totalRefundedPaise, boolean fullyRefunded, Instant at) {}
  public record PaymentStateChangedEvent(UUID intentId, PaymentStatus from, PaymentStatus to, UUID actorId, Instant at) {}
}