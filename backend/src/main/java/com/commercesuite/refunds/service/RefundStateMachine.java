package com.commercesuite.refunds.service;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.refunds.entity.RefundRequest;
import com.commercesuite.refunds.entity.RefundStatus;
import com.commercesuite.refunds.event.RefundEvents.RefundStateChangedEvent;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundStateMachine {
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public void transition(RefundRequest r, RefundStatus next) {
    RefundStatus prev = r.getStatus();
    if (prev == next) return;
    if (!prev.canTransitionTo(next))
      throw AppException.conflict(ErrorCode.CONFLICT, "Illegal refund transition " + prev + " -> " + next);
    r.setStatus(next);
    Instant now = Instant.now(clock);
    if (next == RefundStatus.COMPLETED) r.setCompletedAt(now);
    events.publishEvent(new RefundStateChangedEvent(r.getId(), prev, next, now));
  }
}
