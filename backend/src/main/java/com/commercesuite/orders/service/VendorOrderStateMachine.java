package com.commercesuite.orders.service;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.orders.entity.*;
import com.commercesuite.orders.event.OrderEvents.VendorOrderStateChangedEvent;
import com.commercesuite.orders.repository.OrderStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VendorOrderStateMachine {
  private final OrderStatusHistoryRepository historyRepo;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public void transition(VendorOrder v, VendorOrderStatus next, UUID actorId, String actorRole, String reason) {
    VendorOrderStatus prev = v.getStatus();
    if (prev == next) return;
    if (!prev.canTransitionTo(next))
      throw AppException.conflict(ErrorCode.CONFLICT, "Illegal vendor-order transition " + prev + " -> " + next);
    v.setStatus(next);
    Instant now = Instant.now(clock);
    historyRepo.save(OrderStatusHistory.builder()
        .vendorOrderId(v.getId()).fromStatus(prev.name()).toStatus(next.name())
        .actorId(actorId).actorRole(actorRole).reason(reason).changedAt(now).build());
    events.publishEvent(new VendorOrderStateChangedEvent(v.getId(), v.getOrderId(), v.getVendorId(),
        prev, next, actorId, now));
  }
}
