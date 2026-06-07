package com.commercesuite.checkout.service;

import com.commercesuite.checkout.entity.CheckoutSession;
import com.commercesuite.checkout.entity.CheckoutStatus;
import com.commercesuite.checkout.event.CheckoutEvents.CheckoutStateChangedEvent;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckoutStateMachine {

    private final ApplicationEventPublisher events;
    private final Clock clock;

    public void transition(CheckoutSession s, CheckoutStatus next) {
        CheckoutStatus from = s.getStatus();
        if (from == next) return;
        if (!from.canTransitionTo(next))
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Illegal checkout transition: " + from + " -> " + next);
        s.setStatus(next);
        events.publishEvent(new CheckoutStateChangedEvent(s.getId(), from, next, Instant.now(clock)));
    }

    public void requireActive(CheckoutSession s) {
        if (s.getStatus().isTerminal())
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Checkout is terminal: " + s.getStatus());
        if (s.getExpiresAt() != null && s.getExpiresAt().isBefore(Instant.now(clock)))
            throw AppException.conflict(ErrorCode.CONFLICT, "Checkout expired");
    }
}