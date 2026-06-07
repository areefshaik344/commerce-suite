package com.commercesuite.settlement.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.event.AfterCommitEventPublisher;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.settlement.dto.SettlementDto;
import com.commercesuite.settlement.dto.SettlementLineDto;
import com.commercesuite.settlement.entity.Settlement;
import com.commercesuite.settlement.entity.SettlementLine;
import com.commercesuite.settlement.entity.SettlementStatus;
import com.commercesuite.settlement.event.SettlementEvents.SettlementCalculatedEvent;
import com.commercesuite.settlement.event.SettlementEvents.SettlementLockedEvent;
import com.commercesuite.settlement.event.SettlementEvents.SettlementPaidEvent;
import com.commercesuite.settlement.repository.SettlementLineRepository;
import com.commercesuite.settlement.repository.SettlementRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepo;
    private final SettlementLineRepository lineRepo;
    private final SettlementCalculator calculator;
    private final SettlementStateMachine fsm;
    private final SettlementOwnershipGuard ownership;
    private final AfterCommitEventPublisher events;
    private final Clock clock;

    @Transactional
    public Settlement calculate(UUID vendorId, Instant periodStart, Instant periodEnd, ActorContext actor) {
        if (!periodEnd.isAfter(periodStart))
            throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "periodEnd must be > periodStart");
        Settlement s = settlementRepo.save(Settlement.builder()
                .vendorId(vendorId).status(SettlementStatus.PENDING).currency("INR")
                .periodStart(periodStart).periodEnd(periodEnd)
                .grossPaise(0).refundPaise(0).commissionPaise(0)
                .platformFeePaise(0).adjustmentPaise(0).netPayablePaise(0)
                .build());
        var r = calculator.compute(vendorId, periodStart, periodEnd, s.getId());
        for (SettlementLine l : r.lines()) lineRepo.save(l);
        s.setGrossPaise(r.gross()); s.setRefundPaise(r.refund());
        s.setCommissionPaise(r.commission()); s.setPlatformFeePaise(r.platformFee());
        s.setNetPayablePaise(r.net()); s.setCalculationHash(r.hash());
        fsm.transition(s, SettlementStatus.CALCULATED, actor == null ? null : actor.userId(),
                actor == null ? "system" : "admin", "calculate");
        settlementRepo.save(s);
        events.publish(new SettlementCalculatedEvent(s.getId(), vendorId, r.net(), Instant.now(clock)));
        return s;
    }

    @Transactional
    public Settlement lock(UUID settlementId, ActorContext actor) {
        Settlement s = settlementRepo.findById(settlementId)
                .orElseThrow(() -> AppException.notFound("Settlement"));
        fsm.transition(s, SettlementStatus.LOCKED, actor.userId(), "admin", "lock");
        settlementRepo.save(s);
        events.publish(new SettlementLockedEvent(s.getId(), s.getVendorId(),
                s.getNetPayablePaise(), Instant.now(clock)));
        return s;
    }

    @Transactional
    public Settlement markPaid(UUID settlementId, UUID payoutId, ActorContext actor) {
        Settlement s = settlementRepo.findById(settlementId)
                .orElseThrow(() -> AppException.notFound("Settlement"));
        s.setPayoutId(payoutId);
        fsm.transition(s, SettlementStatus.PAID, actor == null ? null : actor.userId(), "system", "paid");
        settlementRepo.save(s);
        events.publish(new SettlementPaidEvent(s.getId(), s.getVendorId(), payoutId, Instant.now(clock)));
        return s;
    }

    @Transactional(readOnly = true)
    public SettlementDto get(UUID id, ActorContext actor) {
        Settlement s = settlementRepo.findById(id).orElseThrow(() -> AppException.notFound("Settlement"));
        ownership.requireVendorOrAdmin(s, actor);
        return toDto(s);
    }

    @Transactional(readOnly = true)
    public Page<SettlementDto> listForVendor(UUID vendorId, Pageable p) {
        return settlementRepo.findByVendorId(vendorId, p).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<SettlementDto> listAll(Pageable p) {
        return settlementRepo.findAll(p).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<Settlement> findReadyForPayout() {
        return settlementRepo.findByStatus(SettlementStatus.LOCKED);
    }

    public SettlementDto toDto(Settlement s) {
        List<SettlementLineDto> lines = lineRepo.findBySettlementId(s.getId()).stream()
                .map(SettlementLineDto::from).toList();
        return SettlementDto.from(s, lines);
    }
}