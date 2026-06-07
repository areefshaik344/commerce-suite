package com.commercesuite.shipping.entity;
import java.util.Map;
import java.util.Set;
public enum ShipmentStatus {
  CREATED, READY_FOR_PICKUP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED, RETURN_TO_ORIGIN;
  public boolean isTerminal() { return this == DELIVERED || this == RETURN_TO_ORIGIN; }
  private static final Map<ShipmentStatus,Set<ShipmentStatus>> ALLOWED = Map.of(
    CREATED, Set.of(READY_FOR_PICKUP, FAILED),
    READY_FOR_PICKUP, Set.of(IN_TRANSIT, FAILED),
    IN_TRANSIT, Set.of(OUT_FOR_DELIVERY, FAILED, RETURN_TO_ORIGIN),
    OUT_FOR_DELIVERY, Set.of(DELIVERED, FAILED, RETURN_TO_ORIGIN),
    FAILED, Set.of(IN_TRANSIT, RETURN_TO_ORIGIN),
    DELIVERED, Set.of(),
    RETURN_TO_ORIGIN, Set.of()
  );
  public boolean canTransitionTo(ShipmentStatus next) { return ALLOWED.getOrDefault(this, Set.of()).contains(next); }
}
