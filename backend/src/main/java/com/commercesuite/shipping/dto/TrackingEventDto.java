package com.commercesuite.shipping.dto;
import com.commercesuite.shipping.entity.TrackingEvent;
import java.time.Instant;
import java.util.UUID;
public record TrackingEventDto(UUID id, UUID shipmentId, String eventType, String description,
                               String location, Instant occurredAt) {
  public static TrackingEventDto from(TrackingEvent e) {
    return new TrackingEventDto(e.getId(), e.getShipmentId(), e.getEventType(),
        e.getDescription(), e.getLocation(), e.getOccurredAt());
  }
}
