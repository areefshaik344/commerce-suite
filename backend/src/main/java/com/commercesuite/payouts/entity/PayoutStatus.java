package com.commercesuite.payouts.entity;
import java.util.Map;
import java.util.Set;
public enum PayoutStatus {
  CREATED, PROCESSING, COMPLETED, FAILED, CANCELLED;
  public boolean isTerminal() { return this == COMPLETED || this == CANCELLED; }
  private static final Map<PayoutStatus,Set<PayoutStatus>> ALLOWED = Map.of(
    CREATED,    Set.of(PROCESSING, CANCELLED, FAILED),
    PROCESSING, Set.of(COMPLETED, FAILED),
    FAILED,     Set.of(PROCESSING, CANCELLED),
    COMPLETED,  Set.of(),
    CANCELLED,  Set.of()
  );
  public boolean canTransitionTo(PayoutStatus next) { return ALLOWED.getOrDefault(this, Set.of()).contains(next); }
}