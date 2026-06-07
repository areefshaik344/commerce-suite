package com.commercesuite.payouts.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.event.AfterCommitEventPublisher;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.payouts.entity.PayoutStatus;
import com.commercesuite.payouts.entity.VendorPayout;
import com.commercesuite.payouts.event.PayoutEvents.*;
import com.commercesuite.payouts.repository.VendorPayoutRepository;
import com.commercesuite.settlement.entity.Settlement;
import com.commercesuite.settlement.service.SettlementService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayoutService {
  private final VendorPayoutRepository payoutRepo;
  private final PayoutStateMachine fsm;
  private final PayoutOwnershipGuard ownership;
  private final SettlementService settlements;
  private final AfterCommitEventPublisher events;
  private final Clock clock;

  @Transactional
  public VendorPayout createFromSettlement(Settlement s, UUID batchId, ActorContext actor) {
    VendorPayout p = payoutRepo.save(VendorPayout.builder()
        .vendorId(s.getVendorId()).batchId(batchId).settlementId(s.getId())
        .status(PayoutStatus.CREATED).currency("INR").amountPaise(s.getNetPayablePaise())
        .scheduledAt(Instant.now(clock)).build());
    events.publish(new PayoutCreatedEvent(p.getId(), p.getVendorId(), s.getId(),
        p.getAmountPaise(), Instant.now(clock)));
    return p;
  }

  @Transactional
  public VendorPayout markProcessing(UUID payoutId, ActorContext actor) {
    VendorPayout p = load(payoutId);
    fsm.transition(p, PayoutStatus.PROCESSING, actor.userId(), "admin", "process");
    return payoutRepo.save(p);
  }

  @Transactional
  public VendorPayout markCompleted(UUID payoutId, String bankRef, ActorContext actor) {
    VendorPayout p = load(payoutId);
    if (p.getStatus() == PayoutStatus.CREATED) fsm.transition(p, PayoutStatus.PROCESSING, actor.userId(), "admin", "auto");
    p.setBankReference(bankRef);
    fsm.transition(p, PayoutStatus.COMPLETED, actor.userId(), "admin", "completed");
    payoutRepo.save(p);
    settlements.markPaid(p.getSettlementId(), p.getId(), actor);
    events.publish(new PayoutCompletedEvent(p.getId(), p.getVendorId(), p.getAmountPaise(), Instant.now(clock)));
    return p;
  }

  @Transactional
  public VendorPayout markFailed(UUID payoutId, String code, String message, ActorContext actor) {
    VendorPayout p = load(payoutId);
    p.setFailureCode(code); p.setFailureMessage(message);
    fsm.transition(p, PayoutStatus.FAILED, actor == null ? null : actor.userId(), "system", "failed");
    payoutRepo.save(p);
    events.publish(new PayoutFailedEvent(p.getId(), p.getVendorId(), code, message, Instant.now(clock)));
    return p;
  }

  @Transactional
  public VendorPayout cancel(UUID payoutId, ActorContext actor) {
    VendorPayout p = load(payoutId);
    fsm.transition(p, PayoutStatus.CANCELLED, actor.userId(), "admin", "cancel");
    return payoutRepo.save(p);
  }

  @Transactional(readOnly = true)
  public VendorPayout get(UUID id, ActorContext actor) {
    VendorPayout p = load(id); ownership.requireVendorOrAdmin(p, actor); return p;
  }

  @Transactional(readOnly = true)
  public Page<VendorPayout> listForVendor(UUID vendorId, Pageable p) {
    return payoutRepo.findByVendorId(vendorId, p);
  }

  @Transactional(readOnly = true)
  public Page<VendorPayout> listAll(Pageable p) { return payoutRepo.findAll(p); }

  private VendorPayout load(UUID id) {
    return payoutRepo.findById(id).orElseThrow(() -> AppException.notFound("Payout"));
  }
}