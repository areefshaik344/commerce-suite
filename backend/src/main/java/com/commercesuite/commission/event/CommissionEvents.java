package com.commercesuite.commission.event;
import java.time.Instant;
import java.util.UUID;
public final class CommissionEvents {
  private CommissionEvents() {}
  public record CommissionCalculatedEvent(UUID vendorOrderId, UUID vendorId, long commissionPaise, Instant at) {}
  public record CommissionRuleChangedEvent(UUID ruleId, UUID vendorId, boolean active, Instant at) {}
}