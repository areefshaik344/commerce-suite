package com.commercesuite.payments.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.event.AfterCommitEventPublisher;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.payments.dto.*;
import com.commercesuite.payments.entity.*;
import com.commercesuite.payments.event.PaymentEvents.*;
import com.commercesuite.payments.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment orchestration.
 *
 *  - {@link #createIntent} is idempotent on (customer, idempotency_key).
 *  - {@link #retry} produces a new PaymentAttempt and, on success, advances FSM.
 *  - {@link #applyRefund} is invoked by {@code RefundProcessor} to record a
 *    REFUND PaymentTransaction and roll up partial/full refund FSM state.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentIntentRepository intentRepo;
    private final PaymentTransactionRepository txRepo;
    private final PaymentAttemptService attempts;
    private final PaymentStateMachine fsm;
    private final PaymentOwnershipGuard ownership;
    private final AfterCommitEventPublisher events;
    private final Clock clock;

    @Transactional
    public PaymentIntent createIntent(CreatePaymentIntentRequest req, String idempotencyKey, ActorContext actor) {
        Optional<PaymentIntent> existing = intentRepo.findByCustomerIdAndIdempotencyKey(
                actor.userId(), idempotencyKey);
        if (existing.isPresent()) return existing.get();
        PaymentIntent intent = intentRepo.save(PaymentIntent.builder()
                .customerId(actor.userId())
                .checkoutId(req.checkoutId())
                .orderId(req.orderId())
                .status(PaymentStatus.CREATED)
                .currency("INR")
                .amountPaise(req.amountPaise())
                .authorizedPaise(0).capturedPaise(0).refundedPaise(0)
                .methodKind(req.methodKind())
                .paymentMethodId(req.paymentMethodId())
                .gatewayProvider(req.gatewayProvider())
                .idempotencyKey(idempotencyKey)
                .metadata("{}")
                .build());
        events.publish(new PaymentCreatedEvent(intent.getId(), intent.getCustomerId(),
                intent.getOrderId(), intent.getAmountPaise(), Instant.now(clock)));
        return intent;
    }

    @Transactional(readOnly = true)
    public PaymentIntent loadOwned(UUID intentId, ActorContext actor) {
        PaymentIntent i = intentRepo.findById(intentId).orElseThrow(() -> AppException.notFound("Payment"));
        ownership.requireCustomerOrAdmin(i, actor);
        return i;
    }

    @Transactional(readOnly = true)
    public Page<PaymentIntent> listMine(ActorContext actor, Pageable p) {
        return intentRepo.findByCustomerId(actor.userId(), p);
    }

    @Transactional(readOnly = true)
    public Page<PaymentIntent> listAll(Pageable p) { return intentRepo.findAll(p); }

    /**
     * Retry a failed payment by emitting a new attempt. In Phase 7 the sandbox
     * outcome is driven by {@code simulateOutcome}; production adapters will
     * delegate to a gateway client.
     */
    @Transactional
    public PaymentIntent retry(UUID intentId, RetryPaymentRequest req, ActorContext actor) {
        PaymentIntent i = loadOwned(intentId, actor);
        if (i.getStatus() == PaymentStatus.CAPTURED || i.getStatus() == PaymentStatus.AUTHORIZED)
            throw AppException.conflict(ErrorCode.CONFLICT, "Payment is not retryable: " + i.getStatus());
        if (i.getStatus().isTerminal() && i.getStatus() != PaymentStatus.FAILED)
            throw AppException.conflict(ErrorCode.CONFLICT, "Payment terminal: " + i.getStatus());

        String outcome = req == null || req.simulateOutcome() == null ? "SUCCESS" : req.simulateOutcome();
        boolean ok = "SUCCESS".equalsIgnoreCase(outcome);
        PaymentStatus result = ok ? PaymentStatus.CAPTURED : PaymentStatus.FAILED;
        attempts.record(i.getId(), result, req == null ? null : req.gatewayProvider(),
                null, null, null,
                ok ? null : "GATEWAY_DECLINED", ok ? null : "Simulated decline");

        if (ok) {
            // CREATED|FAILED → AUTHORIZED → CAPTURED (FAILED cannot transition; create a sibling intent in real life)
            if (i.getStatus() == PaymentStatus.FAILED) {
                throw AppException.conflict(ErrorCode.CONFLICT,
                        "FAILED payment cannot be retried in place; create a new intent");
            }
            fsm.transition(i, PaymentStatus.AUTHORIZED, actor.userId(), "system", "retry-auth");
            i.setAuthorizedPaise(i.getAmountPaise());
            txRepo.save(PaymentTransaction.builder()
                    .intentId(i.getId()).txType(PaymentTransactionType.AUTHORIZATION)
                    .amountPaise(i.getAmountPaise()).currency("INR")
                    .gatewayProvider(i.getGatewayProvider()).build());
            fsm.transition(i, PaymentStatus.CAPTURED, actor.userId(), "system", "retry-capture");
            i.setCapturedPaise(i.getAmountPaise());
            txRepo.save(PaymentTransaction.builder()
                    .intentId(i.getId()).txType(PaymentTransactionType.CAPTURE)
                    .amountPaise(i.getAmountPaise()).currency("INR")
                    .gatewayProvider(i.getGatewayProvider()).build());
            events.publish(new PaymentAuthorizedEvent(i.getId(), i.getAmountPaise(), Instant.now(clock)));
            events.publish(new PaymentCapturedEvent(i.getId(), i.getOrderId(), i.getAmountPaise(), Instant.now(clock)));
        } else {
            if (i.getStatus() != PaymentStatus.FAILED) {
                fsm.transition(i, PaymentStatus.FAILED, actor.userId(), "system", "retry-failed");
            }
            i.setFailureCode("GATEWAY_DECLINED");
            i.setFailureMessage("Simulated decline");
            events.publish(new PaymentFailedEvent(i.getId(), "GATEWAY_DECLINED",
                    "Simulated decline", Instant.now(clock)));
        }
        return intentRepo.save(i);
    }

    /** First-time confirm path used during order placement (Phase 7 sandbox). */
    @Transactional
    public PaymentIntent confirmCapture(UUID intentId, ActorContext actor) {
        PaymentIntent i = loadOwned(intentId, actor);
        if (i.getStatus() != PaymentStatus.CREATED)
            throw AppException.conflict(ErrorCode.CONFLICT, "Intent not in CREATED: " + i.getStatus());
        fsm.transition(i, PaymentStatus.AUTHORIZED, actor.userId(), "system", "confirm");
        i.setAuthorizedPaise(i.getAmountPaise());
        txRepo.save(PaymentTransaction.builder()
                .intentId(i.getId()).txType(PaymentTransactionType.AUTHORIZATION)
                .amountPaise(i.getAmountPaise()).currency("INR")
                .gatewayProvider(i.getGatewayProvider()).build());
        fsm.transition(i, PaymentStatus.CAPTURED, actor.userId(), "system", "capture");
        i.setCapturedPaise(i.getAmountPaise());
        txRepo.save(PaymentTransaction.builder()
                .intentId(i.getId()).txType(PaymentTransactionType.CAPTURE)
                .amountPaise(i.getAmountPaise()).currency("INR")
                .gatewayProvider(i.getGatewayProvider()).build());
        events.publish(new PaymentAuthorizedEvent(i.getId(), i.getAmountPaise(), Instant.now(clock)));
        events.publish(new PaymentCapturedEvent(i.getId(), i.getOrderId(), i.getAmountPaise(), Instant.now(clock)));
        return intentRepo.save(i);
    }

    @Transactional
    public PaymentIntent cancel(UUID intentId, String reason, ActorContext actor) {
        PaymentIntent i = loadOwned(intentId, actor);
        if (!i.getStatus().canTransitionTo(PaymentStatus.CANCELLED))
            throw AppException.conflict(ErrorCode.CONFLICT, "Cannot cancel in state " + i.getStatus());
        fsm.transition(i, PaymentStatus.CANCELLED, actor.userId(), "customer", reason);
        events.publish(new PaymentCancelledEvent(i.getId(), actor.userId(), reason, Instant.now(clock)));
        return intentRepo.save(i);
    }

    /**
     * Apply a refund of {@code amountPaise} to {@code intentId}. Records an
     * immutable REFUND PaymentTransaction, increments {@code refundedPaise},
     * and rolls up FSM to PARTIALLY_REFUNDED / REFUNDED. MoneySpec §4 enforced.
     */
    @Transactional
    public PaymentTransaction applyRefund(UUID intentId, long amountPaise, String gatewayProvider,
                                          String gatewayRef, UUID actorId) {
        if (amountPaise <= 0)
            throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Refund amount must be > 0");
        PaymentIntent i = intentRepo.findById(intentId)
                .orElseThrow(() -> AppException.notFound("Payment"));
        if (i.getStatus() != PaymentStatus.CAPTURED && i.getStatus() != PaymentStatus.PARTIALLY_REFUNDED)
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Cannot refund payment in " + i.getStatus());
        long remaining = i.refundableRemainingPaise();
        if (amountPaise > remaining)
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Refund " + amountPaise + " exceeds refundable balance " + remaining);

        PaymentTransaction tx = txRepo.save(PaymentTransaction.builder()
                .intentId(i.getId()).txType(PaymentTransactionType.REFUND)
                .amountPaise(amountPaise).currency("INR")
                .gatewayProvider(gatewayProvider).gatewayReference(gatewayRef)
                .build());

        long newRefunded = i.getRefundedPaise() + amountPaise;
        i.setRefundedPaise(newRefunded);
        boolean fully = newRefunded >= i.getCapturedPaise();
        PaymentStatus target = fully ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        fsm.transition(i, target, actorId, "system", "refund");
        intentRepo.save(i);
        events.publish(new PaymentRefundedEvent(i.getId(), amountPaise, newRefunded, fully, Instant.now(clock)));
        return tx;
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> transactionsOf(UUID intentId) {
        return txRepo.findByIntentIdOrderByOccurredAtAsc(intentId);
    }
}