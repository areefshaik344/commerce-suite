package com.commercesuite.settlement.event;
import com.commercesuite.settlement.entity.SettlementStatus;
import java.time.Instant;
import java.util.UUID;

public final class SettlementEvents {
  private SettlementEvents() {}
  public record SettlementCalculatedEvent(UUID settlementId, UUID vendorId, long netPayablePaise, Instant at) {}
  public record SettlementLockedEvent(UUID settlementId, UUID vendorId, long netPayablePaise, Instant at) {}
  public record SettlementPaidEvent(UUID settlementId, UUID vendorId, UUID payoutId, Instant at) {}
  public record SettlementStateChangedEvent(UUID settlementId, SettlementStatus from, SettlementStatus to, UUID actorId, Instant at) {}
}