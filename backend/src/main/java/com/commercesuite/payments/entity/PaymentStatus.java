package com.commercesuite.payments.entity;
import java.util.Map;
import java.util.Set;

/** Payment FSM per Phase 7 spec. */
public enum PaymentStatus {
  CREATED, AUTHORIZED, CAPTURED, FAILED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED;

  public boolean isTerminal() {
    return this == FAILED || this == CANCELLED || this == REFUNDED;
  }

  private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = Map.of(
    CREATED,            Set.of(AUTHORIZED, CAPTURED, FAILED, CANCELLED),
    AUTHORIZED,         Set.of(CAPTURED, FAILED, CANCELLED),
    CAPTURED,           Set.of(PARTIALLY_REFUNDED, REFUNDED),
    PARTIALLY_REFUNDED, Set.of(PARTIALLY_REFUNDED, REFUNDED),
    FAILED,             Set.of(),
    CANCELLED,          Set.of(),
    REFUNDED,           Set.of()
  );

  public boolean canTransitionTo(PaymentStatus next) {
    return ALLOWED.getOrDefault(this, Set.of()).contains(next);
  }
}