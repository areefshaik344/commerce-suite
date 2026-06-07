package com.commercesuite.refunds.entity;
import java.util.Map;
import java.util.Set;
public enum RefundStatus {
  PENDING, APPROVED, PROCESSING, COMPLETED, REJECTED;
  public boolean isTerminal() { return this == COMPLETED || this == REJECTED; }
  private static final Map<RefundStatus,Set<RefundStatus>> ALLOWED = Map.of(
    PENDING, Set.of(APPROVED, REJECTED),
    APPROVED, Set.of(PROCESSING, REJECTED),
    PROCESSING, Set.of(COMPLETED, REJECTED),
    COMPLETED, Set.of(),
    REJECTED, Set.of()
  );
  public boolean canTransitionTo(RefundStatus next) { return ALLOWED.getOrDefault(this, Set.of()).contains(next); }
}
