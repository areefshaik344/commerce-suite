package com.commercesuite.payouts.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.event.AfterCommitEventPublisher;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.payouts.entity.PayoutStatus;
import com.commercesuite.payouts.entity.PayoutStatusHistory;
import com.commercesuite.payouts.entity.VendorPayout;
import com.commercesuite.payouts.event.PayoutEvents.PayoutStateChangedEvent;
import com.commercesuite.payouts.repository.PayoutStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayoutStateMachine {
  private final PayoutStatusHistoryRepository historyRepo;
  private final AfterCommitEventPublisher events;
  private final Clock clock;

  public void transition(VendorPayout p, PayoutStatus next, UUID actorId, String actorRole, String reason) {
    PayoutStatus prev = p.getStatus();
    if (prev == next) return;
    if (!prev.canTransitionTo(next))
      throw AppException.conflict(ErrorCode.CONFLICT, "Illegal payout transition " + prev + " -> " + next);
    p.setStatus(next);
    Instant now = Instant.now(clock);
    if (next == PayoutStatus.PROCESSING) p.setProcessedAt(now);
    if (next == PayoutStatus.COMPLETED)  p.setCompletedAt(now);
    historyRepo.save(PayoutStatusHistory.builder()
        .payoutId(p.getId()).fromStatus(prev).toStatus(next)
        .actorId(actorId).actorRole(actorRole).reason(reason).changedAt(now).build());
    events.publish(new PayoutStateChangedEvent(p.getId(), prev, next, actorId, now));
  }
}