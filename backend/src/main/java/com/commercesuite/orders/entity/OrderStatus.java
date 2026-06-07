package com.commercesuite.orders.entity;
import java.util.Map;
import java.util.Set;
public enum OrderStatus {
  CREATED, CONFIRMED, PROCESSING, PARTIALLY_SHIPPED, SHIPPED,
  DELIVERED, PARTIALLY_CANCELLED, CANCELLED, PARTIALLY_RETURNED, RETURNED, CLOSED;
  public boolean isTerminal() { return this == CANCELLED || this == RETURNED || this == CLOSED; }
  private static final Map<OrderStatus,Set<OrderStatus>> ALLOWED = Map.ofEntries(
    Map.entry(CREATED, Set.of(CONFIRMED, CANCELLED)),
    Map.entry(CONFIRMED, Set.of(PROCESSING, PARTIALLY_CANCELLED, CANCELLED)),
    Map.entry(PROCESSING, Set.of(PARTIALLY_SHIPPED, SHIPPED, PARTIALLY_CANCELLED, CANCELLED)),
    Map.entry(PARTIALLY_SHIPPED, Set.of(SHIPPED, DELIVERED, PARTIALLY_CANCELLED)),
    Map.entry(SHIPPED, Set.of(DELIVERED)),
    Map.entry(DELIVERED, Set.of(PARTIALLY_RETURNED, RETURNED, CLOSED)),
    Map.entry(PARTIALLY_CANCELLED, Set.of(PROCESSING, PARTIALLY_SHIPPED, SHIPPED, DELIVERED, CANCELLED, PARTIALLY_RETURNED, CLOSED)),
    Map.entry(PARTIALLY_RETURNED, Set.of(RETURNED, CLOSED)),
    Map.entry(RETURNED, Set.of(CLOSED)),
    Map.entry(CANCELLED, Set.of()),
    Map.entry(CLOSED, Set.of())
  );
  public boolean canTransitionTo(OrderStatus next) { return ALLOWED.getOrDefault(this, Set.of()).contains(next); }
}
