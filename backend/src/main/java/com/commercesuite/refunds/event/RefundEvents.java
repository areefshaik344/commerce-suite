package com.commercesuite.refunds.event;
import com.commercesuite.refunds.entity.RefundStatus;
import java.time.Instant;
import java.util.UUID;

public final class RefundEvents {
  private RefundEvents() {}
  public record RefundRequestedEvent(UUID refundId, UUID orderId, String sourceType, UUID sourceId,
                                     long amountPaise, Instant at) {}
  public record RefundStateChangedEvent(UUID refundId, RefundStatus from, RefundStatus to, Instant at) {}
  public record RefundApprovedEvent(UUID refundId, long amountPaise, UUID actorId, Instant at) {}
  public record RefundCompletedEvent(UUID refundId, long amountPaise, Instant at) {}
  public record RefundRejectedEvent(UUID refundId, UUID actorId, String reason, Instant at) {}
}
