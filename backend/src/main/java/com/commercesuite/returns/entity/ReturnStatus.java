package com.commercesuite.returns.entity;
import java.util.Map;
import java.util.Set;
public enum ReturnStatus {
  REQUESTED, APPROVED, REJECTED, RECEIVED, COMPLETED;
  public boolean isTerminal() { return this == REJECTED || this == COMPLETED; }
  private static final Map<ReturnStatus,Set<ReturnStatus>> ALLOWED = Map.of(
    REQUESTED, Set.of(APPROVED, REJECTED),
    APPROVED,  Set.of(RECEIVED, REJECTED),
    RECEIVED,  Set.of(COMPLETED),
    REJECTED,  Set.of(),
    COMPLETED, Set.of()
  );
  public boolean canTransitionTo(ReturnStatus next) { return ALLOWED.getOrDefault(this, Set.of()).contains(next); }
}
