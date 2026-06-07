package com.commercesuite.settlement.entity;
import java.util.Map;
import java.util.Set;
public enum SettlementStatus {
  PENDING, CALCULATED, LOCKED, PAID;
  public boolean isTerminal() { return this == PAID; }
  private static final Map<SettlementStatus,Set<SettlementStatus>> ALLOWED = Map.of(
    PENDING,    Set.of(CALCULATED),
    CALCULATED, Set.of(LOCKED, PENDING),
    LOCKED,     Set.of(PAID),
    PAID,       Set.of()
  );
  public boolean canTransitionTo(SettlementStatus next) { return ALLOWED.getOrDefault(this, Set.of()).contains(next); }
}