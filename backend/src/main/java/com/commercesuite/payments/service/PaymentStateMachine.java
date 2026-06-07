package com.commercesuite.payments.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.event.AfterCommitEventPublisher;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.payments.entity.*;
import com.commercesuite.payments.event.PaymentEvents.PaymentStateChangedEvent;
import com.commercesuite.payments.repository.PaymentStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Payment FSM. DB trigger {@code fn_assert_payment_transition} mirrors this. */
@Component
@RequiredArgsConstructor
public class PaymentStateMachine {
  private final PaymentStatusHistoryRepository historyRepo;
  private final AfterCommitEventPublisher events;
  private final Clock clock;

  public void transition(PaymentIntent intent, PaymentStatus next, UUID actorId, String actorRole, String reason) {
    PaymentStatus prev = intent.getStatus();
    if (prev == next && prev != PaymentStatus.PARTIALLY_REFUNDED) return;
    if (!prev.canTransitionTo(next))
      throw AppException.conflict(ErrorCode.CONFLICT, "Illegal payment transition " + prev + " -> " + next);
    intent.setStatus(next);
    Instant now = Instant.now(clock);
    switch (next) {
      case AUTHORIZED -> intent.setAuthorizedAt(now);
      case CAPTURED   -> intent.setCapturedAt(now);
      case FAILED     -> intent.setFailedAt(now);
      case CANCELLED  -> intent.setCancelledAt(now);
      default -> {}
    }
    historyRepo.save(PaymentStatusHistory.builder()
        .intentId(intent.getId()).fromStatus(prev).toStatus(next)
        .actorId(actorId).actorRole(actorRole).reason(reason).changedAt(now).build());
    events.publish(new PaymentStateChangedEvent(intent.getId(), prev, next, actorId, now));
  }
}