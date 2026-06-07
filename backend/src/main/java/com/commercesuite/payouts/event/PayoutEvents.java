package com.commercesuite.payouts.event;
import com.commercesuite.payouts.entity.PayoutStatus;
import java.time.Instant;
import java.util.UUID;

public final class PayoutEvents {
  private PayoutEvents() {}
  public record PayoutCreatedEvent(UUID payoutId, UUID vendorId, UUID settlementId, long amountPaise, Instant at) {}
  public record PayoutCompletedEvent(UUID payoutId, UUID vendorId, long amountPaise, Instant at) {}
  public record PayoutFailedEvent(UUID payoutId, UUID vendorId, String code, String message, Instant at) {}
  public record PayoutStateChangedEvent(UUID payoutId, PayoutStatus from, PayoutStatus to, UUID actorId, Instant at) {}
  public record PayoutBatchCreatedEvent(UUID batchId, int payoutCount, long totalPaise, Instant at) {}
  public record PayoutBatchCompletedEvent(UUID batchId, Instant at) {}
}