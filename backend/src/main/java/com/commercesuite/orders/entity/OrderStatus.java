package com.commercesuite.orders.entity;
import java.util.Map;
import java.util.Set;
/**
 * Parent Order FSM — derived rollup of vendor-order children.
 * Authoritative spec: docs/ORDER_FSM.md §3.
 *
 * `PENDING_PAYMENT` is the initial state (was `CREATED`; alias retained for
 * back-compat until Phase 7 payments wires CREATED→PENDING_PAYMENT).
 * `COMPLETED` replaces the prior `CLOSED` terminal (alias retained).
 * `PARTIALLY_DELIVERED` added per spec rollup row 6.
 */
public enum OrderStatus {
  PENDING_PAYMENT, CREATED, CONFIRMED, PROCESSING, PARTIALLY_SHIPPED, SHIPPED,
  PARTIALLY_DELIVERED, DELIVERED, PARTIALLY_CANCELLED, CANCELLED,
  PARTIALLY_RETURNED, RETURNED, COMPLETED, CLOSED;
  public boolean isTerminal() {
    return this == CANCELLED || this == RETURNED || this == COMPLETED || this == CLOSED;
  }
  private static final Map<OrderStatus,Set<OrderStatus>> ALLOWED = Map.ofEntries(
    Map.entry(PENDING_PAYMENT, Set.of(CONFIRMED, CANCELLED)),
    Map.entry(CREATED, Set.of(CONFIRMED, CANCELLED)),
    Map.entry(CONFIRMED, Set.of(PROCESSING, PARTIALLY_CANCELLED, CANCELLED)),
    Map.entry(PROCESSING, Set.of(PARTIALLY_SHIPPED, SHIPPED, PARTIALLY_CANCELLED, CANCELLED)),
    Map.entry(PARTIALLY_SHIPPED, Set.of(SHIPPED, PARTIALLY_DELIVERED, DELIVERED, PARTIALLY_CANCELLED)),
    Map.entry(SHIPPED, Set.of(PARTIALLY_DELIVERED, DELIVERED)),
    Map.entry(PARTIALLY_DELIVERED, Set.of(DELIVERED, PARTIALLY_RETURNED, PARTIALLY_CANCELLED)),
    Map.entry(DELIVERED, Set.of(PARTIALLY_RETURNED, RETURNED, COMPLETED, CLOSED)),
    Map.entry(PARTIALLY_CANCELLED, Set.of(PROCESSING, PARTIALLY_SHIPPED, SHIPPED, PARTIALLY_DELIVERED,
        DELIVERED, CANCELLED, PARTIALLY_RETURNED, COMPLETED, CLOSED)),
    Map.entry(PARTIALLY_RETURNED, Set.of(RETURNED, COMPLETED, CLOSED)),
    Map.entry(RETURNED, Set.of(COMPLETED, CLOSED)),
    Map.entry(CANCELLED, Set.of()),
    Map.entry(COMPLETED, Set.of()),
    Map.entry(CLOSED, Set.of())
  );
  public boolean canTransitionTo(OrderStatus next) { return ALLOWED.getOrDefault(this, Set.of()).contains(next); }
}
