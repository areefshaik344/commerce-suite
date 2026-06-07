package com.commercesuite.returns.event;
import com.commercesuite.returns.entity.ReturnStatus;
import java.time.Instant;
import java.util.UUID;

public final class ReturnEvents {
  private ReturnEvents() {}
  public record ReturnRequestedEvent(UUID returnId, UUID orderId, UUID vendorOrderId, UUID vendorId,
                                     UUID customerId, long refundPaise, Instant at) {}
  public record ReturnStateChangedEvent(UUID returnId, ReturnStatus from, ReturnStatus to, UUID actorId, Instant at) {}
  public record ReturnApprovedEvent(UUID returnId, UUID actorId, Instant at) {}
  public record ReturnRejectedEvent(UUID returnId, UUID actorId, String reason, Instant at) {}
  public record ReturnCompletedEvent(UUID returnId, long refundPaise, Instant at) {}
}
