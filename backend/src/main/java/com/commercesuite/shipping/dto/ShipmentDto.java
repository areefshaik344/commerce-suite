package com.commercesuite.shipping.dto;
import com.commercesuite.shipping.entity.Shipment;
import com.commercesuite.shipping.entity.ShipmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record ShipmentDto(UUID id, UUID orderId, UUID vendorOrderId, UUID vendorId,
                          ShipmentStatus status, String carrier, String trackingNumber,
                          String shippingMethod, long shippingPaise,
                          Instant shippedAt, Instant deliveredAt, Instant estimatedDeliveryAt,
                          List<ShipmentItemDto> items) {
  public static ShipmentDto from(Shipment s, List<ShipmentItemDto> items) {
    return new ShipmentDto(s.getId(), s.getOrderId(), s.getVendorOrderId(), s.getVendorId(),
        s.getStatus(), s.getCarrier(), s.getTrackingNumber(), s.getShippingMethod(),
        s.getShippingPaise(), s.getShippedAt(), s.getDeliveredAt(), s.getEstimatedDeliveryAt(), items);
  }
}
