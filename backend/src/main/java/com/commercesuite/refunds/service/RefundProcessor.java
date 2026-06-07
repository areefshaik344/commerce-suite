package com.commercesuite.refunds.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.payments.entity.PaymentIntent;
import com.commercesuite.payments.entity.PaymentStatus;
import com.commercesuite.payments.repository.PaymentIntentRepository;
import com.commercesuite.payments.service.PaymentService;
import com.commercesuite.refunds.entity.RefundRequest;
import com.commercesuite.refunds.entity.RefundStatus;
import com.commercesuite.refunds.entity.RefundTransaction;
import com.commercesuite.refunds.event.RefundEvents.RefundCompletedEvent;
import com.commercesuite.refunds.repository.RefundRequestRepository;
import com.commercesuite.refunds.repository.RefundTransactionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Connects Phase 6 RefundRequest lifecycle to Phase 7 PaymentService.
 *
 *  - {@link #process(UUID, long, ActorContext)} refunds a partial or full
 *    amount against the order's captured payment, records an internal
 *    RefundTransaction, advances the refund FSM PENDING → APPROVED →
 *    PROCESSING → COMPLETED, and emits {@link RefundCompletedEvent}.
 *  - Supports multiple partial refunds against the same RefundRequest as long
 *    as the cumulative amount does not exceed the request total.
 */
@Service
@RequiredArgsConstructor
public class RefundProcessor {

    private final RefundRequestRepository refundRepo;
    private final RefundTransactionRepository txRepo;
    private final RefundStateMachine fsm;
    private final PaymentIntentRepository intentRepo;
    private final PaymentService payments;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public RefundTransaction process(UUID refundId, long amountPaise, ActorContext actor) {
        RefundRequest r = refundRepo.findById(refundId)
                .orElseThrow(() -> AppException.notFound("Refund"));
        if (amountPaise <= 0)
            throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Refund amount must be > 0");
        long already = txRepo.findByRefundId(refundId).stream()
                .filter(t -> t.getStatus() == RefundStatus.COMPLETED)
                .mapToLong(RefundTransaction::getAmountPaise).sum();
        if (already + amountPaise > r.getAmountPaise())
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Refund " + amountPaise + " exceeds remaining refundable of request ("
                            + (r.getAmountPaise() - already) + ")");

        // Locate captured intent for the order
        List<PaymentIntent> intents = intentRepo.findByOrderId(r.getOrderId());
        PaymentIntent target = intents.stream()
                .filter(i -> i.getStatus() == PaymentStatus.CAPTURED
                          || i.getStatus() == PaymentStatus.PARTIALLY_REFUNDED)
                .findFirst()
                .orElseThrow(() -> AppException.conflict(ErrorCode.CONFLICT,
                        "No refundable PaymentIntent for order " + r.getOrderId()));

        if (r.getStatus() == RefundStatus.PENDING)   fsm.transition(r, RefundStatus.APPROVED);
        if (r.getStatus() == RefundStatus.APPROVED)  fsm.transition(r, RefundStatus.PROCESSING);

        // Apply against payment intent (records PaymentTransaction REFUND + updates FSM)
        var paymentTx = payments.applyRefund(target.getId(), amountPaise,
                target.getGatewayProvider(), null, actor.userId());

        RefundTransaction rtx = txRepo.save(RefundTransaction.builder()
                .refundId(r.getId()).amountPaise(amountPaise)
                .status(RefundStatus.COMPLETED)
                .gatewayRef(paymentTx.getId().toString())
                .processedAt(Instant.now(clock))
                .build());

        if (already + amountPaise >= r.getAmountPaise()) {
            fsm.transition(r, RefundStatus.COMPLETED);
            refundRepo.save(r);
            events.publishEvent(new RefundCompletedEvent(r.getId(), r.getAmountPaise(), Instant.now(clock)));
        }
        return rtx;
    }
}