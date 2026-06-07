package com.commercesuite.orders.event;
import com.commercesuite.orders.entity.OrderStatus;
import com.commercesuite.orders.entity.VendorOrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderEvents {
  private OrderEvents() {}
  public record OrderCreatedEvent(UUID orderId, UUID customerId, UUID checkoutId,
                                  List<UUID> vendorOrderIds, long grandTotalPaise, Instant at) {}
  public record OrderStateChangedEvent(UUID orderId, OrderStatus from, OrderStatus to, UUID actorId, Instant at) {}
  public record VendorOrderStateChangedEvent(UUID vendorOrderId, UUID orderId, UUID vendorId,
                                             VendorOrderStatus from, VendorOrderStatus to, UUID actorId, Instant at) {}
  public record OrderCancelledEvent(UUID orderId, UUID actorId, String reason, Instant at) {}
  public record OrderDeliveredEvent(UUID orderId, Instant at) {}
}
