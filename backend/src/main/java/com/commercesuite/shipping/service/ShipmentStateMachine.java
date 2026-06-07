package com.commercesuite.shipping.service;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.shipping.entity.Shipment;
import com.commercesuite.shipping.entity.ShipmentStatus;
import com.commercesuite.shipping.event.ShippingEvents.ShipmentStateChangedEvent;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShipmentStateMachine {
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public void transition(Shipment s, ShipmentStatus next) {
    ShipmentStatus prev = s.getStatus();
    if (prev == next) return;
    if (!prev.canTransitionTo(next))
      throw AppException.conflict(ErrorCode.CONFLICT, "Illegal shipment transition " + prev + " -> " + next);
    s.setStatus(next);
    Instant now = Instant.now(clock);
    if (next == ShipmentStatus.IN_TRANSIT && s.getShippedAt() == null) s.setShippedAt(now);
    if (next == ShipmentStatus.DELIVERED) s.setDeliveredAt(now);
    events.publishEvent(new ShipmentStateChangedEvent(s.getId(), prev, next, now));
  }
}
