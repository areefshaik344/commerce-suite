package com.commercesuite.orders.entity;
import java.util.Map;
import java.util.Set;
public enum VendorOrderStatus {
  CREATED, CONFIRMED, PROCESSING, PACKED, SHIPPED, OUT_FOR_DELIVERY,
  DELIVERED, CANCELLED, RETURN_REQUESTED, RETURNED, REFUNDED, CLOSED;
  public boolean isTerminal() { return this == CANCELLED || this == REFUNDED || this == CLOSED; }
  public boolean isCancellable() {
    return this == CREATED || this == CONFIRMED || this == PROCESSING || this == PACKED;
  }
  public boolean isDelivered() { return this == DELIVERED; }
  private static final Map<VendorOrderStatus,Set<VendorOrderStatus>> ALLOWED = Map.ofEntries(
    Map.entry(CREATED, Set.of(CONFIRMED, CANCELLED)),
    Map.entry(CONFIRMED, Set.of(PROCESSING, CANCELLED)),
    Map.entry(PROCESSING, Set.of(PACKED, CANCELLED)),
    Map.entry(PACKED, Set.of(SHIPPED, CANCELLED)),
    Map.entry(SHIPPED, Set.of(OUT_FOR_DELIVERY, DELIVERED)),
    Map.entry(OUT_FOR_DELIVERY, Set.of(DELIVERED)),
    Map.entry(DELIVERED, Set.of(RETURN_REQUESTED, CLOSED)),
    Map.entry(RETURN_REQUESTED, Set.of(RETURNED, DELIVERED)),
    Map.entry(RETURNED, Set.of(REFUNDED)),
    Map.entry(REFUNDED, Set.of(CLOSED)),
    Map.entry(CANCELLED, Set.of()),
    Map.entry(CLOSED, Set.of())
  );
  public boolean canTransitionTo(VendorOrderStatus next) { return ALLOWED.getOrDefault(this, Set.of()).contains(next); }
}
