package com.commercesuite.returns.service;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.returns.entity.ReturnRequest;
import com.commercesuite.returns.entity.ReturnStatus;
import com.commercesuite.returns.event.ReturnEvents.ReturnStateChangedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReturnStateMachine {
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public void transition(ReturnRequest r, ReturnStatus next, UUID actorId) {
    ReturnStatus prev = r.getStatus();
    if (prev == next) return;
    if (!prev.canTransitionTo(next))
      throw AppException.conflict(ErrorCode.CONFLICT, "Illegal return transition " + prev + " -> " + next);
    r.setStatus(next);
    Instant now = Instant.now(clock);
    if (next == ReturnStatus.RECEIVED && r.getReceivedAt() == null) r.setReceivedAt(now);
    if (next.isTerminal()) r.setResolvedAt(now);
    events.publishEvent(new ReturnStateChangedEvent(r.getId(), prev, next, actorId, now));
  }
}
