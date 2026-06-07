package com.commercesuite.refunds.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.refunds.dto.*;
import com.commercesuite.refunds.entity.*;
import com.commercesuite.refunds.event.RefundEvents.*;
import com.commercesuite.refunds.repository.*;
import com.commercesuite.returns.entity.ReturnItem;
import com.commercesuite.returns.entity.ReturnRequest;
import com.commercesuite.returns.repository.ReturnItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRequestRepository refundRepo;
    private final RefundItemRepository refundItemRepo;
    private final RefundTransactionRepository txRepo;
    private final ReturnItemRepository returnItemRepo;
    private final RefundStateMachine fsm;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public RefundRequest createForReturn(ReturnRequest r) {
        RefundRequest rr = refundRepo.save(RefundRequest.builder()
                .orderId(r.getOrderId()).vendorOrderId(r.getVendorOrderId())
                .sourceType(RefundSourceType.RETURN).sourceId(r.getId())
                .amountPaise(r.getRefundPaise())
                .status(RefundStatus.PENDING)
                .reason("Return completed: " + r.getReason().name())
                .requestedAt(Instant.now(clock)).build());
        for (ReturnItem ri : returnItemRepo.findByReturnId(r.getId())) {
            refundItemRepo.save(RefundItem.builder()
                    .refundId(rr.getId()).orderItemId(ri.getOrderItemId())
                    .qty(ri.getQty()).amountPaise(ri.getRefundPaise()).build());
        }
        events.publishEvent(new RefundRequestedEvent(rr.getId(), r.getOrderId(),
                RefundSourceType.RETURN.name(), r.getId(), r.getRefundPaise(), Instant.now(clock)));
        return rr;
    }

    @Transactional
    public RefundRequestDto approve(UUID refundId, ActorContext actor) {
        RefundRequest r = load(refundId);
        fsm.transition(r, RefundStatus.APPROVED);
        refundRepo.save(r);
        events.publishEvent(new RefundApprovedEvent(r.getId(), r.getAmountPaise(), actor.userId(),
                Instant.now(clock)));
        return toDto(r);
    }

    @Transactional
    public RefundRequestDto reject(UUID refundId, RefundDecisionRequest req, ActorContext actor) {
        RefundRequest r = load(refundId);
        fsm.transition(r, RefundStatus.REJECTED);
        if (req != null) r.setReason(req.reason());
        refundRepo.save(r);
        events.publishEvent(new RefundRejectedEvent(r.getId(), actor.userId(),
                req == null ? null : req.reason(), Instant.now(clock)));
        return toDto(r);
    }

    /**
     * Mark a refund as processed. No payment-gateway integration in Phase 6 —
     * records an internal RefundTransaction and transitions PENDING/APPROVED → PROCESSING → COMPLETED.
     */
    @Transactional
    public RefundRequestDto markCompleted(UUID refundId, ActorContext actor) {
        RefundRequest r = load(refundId);
        if (r.getStatus() == RefundStatus.PENDING) fsm.transition(r, RefundStatus.APPROVED);
        if (r.getStatus() == RefundStatus.APPROVED) fsm.transition(r, RefundStatus.PROCESSING);
        fsm.transition(r, RefundStatus.COMPLETED);
        txRepo.save(RefundTransaction.builder()
                .refundId(r.getId()).amountPaise(r.getAmountPaise())
                .status(RefundStatus.COMPLETED).processedAt(Instant.now(clock))
                .build());
        refundRepo.save(r);
        events.publishEvent(new RefundCompletedEvent(r.getId(), r.getAmountPaise(), Instant.now(clock)));
        return toDto(r);
    }

    @Transactional(readOnly = true)
    public RefundRequestDto get(UUID refundId) { return toDto(load(refundId)); }

    @Transactional(readOnly = true)
    public Page<RefundRequestDto> listAll(Pageable p) {
        return refundRepo.findAll(p).map(this::toDto);
    }

    private RefundRequest load(UUID id) {
        return refundRepo.findById(id).orElseThrow(() -> AppException.notFound("Refund"));
    }

    private RefundRequestDto toDto(RefundRequest r) {
        List<RefundItemDto> items = refundItemRepo.findByRefundId(r.getId()).stream()
                .map(RefundItemDto::from).toList();
        return RefundRequestDto.from(r, items);
    }
}
