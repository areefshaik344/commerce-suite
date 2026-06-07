package com.commercesuite.shipping.event;
import com.commercesuite.shipping.entity.ShipmentStatus;
import java.time.Instant;
import java.util.UUID;

public final class ShippingEvents {
  private ShippingEvents() {}
  public record ShipmentCreatedEvent(UUID shipmentId, UUID vendorOrderId, UUID orderId, UUID vendorId, Instant at) {}
  public record ShipmentStateChangedEvent(UUID shipmentId, ShipmentStatus from, ShipmentStatus to, Instant at) {}
  public record ShipmentDeliveredEvent(UUID shipmentId, UUID orderId, Instant at) {}
  public record TrackingEventRecordedEvent(UUID trackingEventId, UUID shipmentId, String eventType, Instant at) {}
}
