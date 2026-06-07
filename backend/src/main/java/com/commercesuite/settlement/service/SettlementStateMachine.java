package com.commercesuite.settlement.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.event.AfterCommitEventPublisher;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.settlement.entity.Settlement;
import com.commercesuite.settlement.entity.SettlementStatus;
import com.commercesuite.settlement.entity.SettlementStatusHistory;
import com.commercesuite.settlement.event.SettlementEvents.SettlementStateChangedEvent;
import com.commercesuite.settlement.repository.SettlementStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementStateMachine {
  private final SettlementStatusHistoryRepository historyRepo;
  private final AfterCommitEventPublisher events;
  private final Clock clock;

  public void transition(Settlement s, SettlementStatus next, UUID actorId, String actorRole, String reason) {
    SettlementStatus prev = s.getStatus();
    if (prev == next) return;
    if (!prev.canTransitionTo(next))
      throw AppException.conflict(ErrorCode.CONFLICT, "Illegal settlement transition " + prev + " -> " + next);
    s.setStatus(next);
    Instant now = Instant.now(clock);
    if (next == SettlementStatus.LOCKED) s.setLockedAt(now);
    if (next == SettlementStatus.PAID)   s.setPaidAt(now);
    historyRepo.save(SettlementStatusHistory.builder()
        .settlementId(s.getId()).fromStatus(prev).toStatus(next)
        .actorId(actorId).actorRole(actorRole).reason(reason).changedAt(now).build());
    events.publish(new SettlementStateChangedEvent(s.getId(), prev, next, actorId, now));
  }
}