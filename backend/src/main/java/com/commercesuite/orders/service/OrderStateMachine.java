package com.commercesuite.orders.service;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.orders.entity.*;
import com.commercesuite.orders.event.OrderEvents.OrderStateChangedEvent;
import com.commercesuite.orders.repository.OrderStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStateMachine {
  private final OrderStatusHistoryRepository historyRepo;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public void transition(Order o, OrderStatus next, UUID actorId, String actorRole, String reason) {
    OrderStatus prev = o.getStatus();
    if (prev == next) return;
    if (!prev.canTransitionTo(next))
      throw AppException.conflict(ErrorCode.CONFLICT, "Illegal order transition " + prev + " -> " + next);
    o.setStatus(next);
    Instant now = Instant.now(clock);
    historyRepo.save(OrderStatusHistory.builder()
        .orderId(o.getId()).fromStatus(prev.name()).toStatus(next.name())
        .actorId(actorId).actorRole(actorRole).reason(reason).changedAt(now).build());
    if (next == OrderStatus.CANCELLED) o.setCancelledAt(now);
    if (next == OrderStatus.DELIVERED) o.setDeliveredAt(now);
    events.publishEvent(new OrderStateChangedEvent(o.getId(), prev, next, actorId, now));
  }
}
