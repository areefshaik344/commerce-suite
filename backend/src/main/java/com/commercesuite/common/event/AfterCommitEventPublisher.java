package com.commercesuite.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * BLOCKER B-06 resolution.
 *
 * Wraps {@link ApplicationEventPublisher} so domain events fire only after
 * the outer transaction commits. Outside a transaction it publishes
 * immediately. Inside an active transaction it defers via
 * {@link TransactionSynchronizationManager}.
 *
 * All new domain-event emit sites SHOULD use this publisher. Listeners
 * authored from Phase 7 onward MUST use
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)}.
 */
@Component
@RequiredArgsConstructor
public class AfterCommitEventPublisher {

    private final ApplicationEventPublisher delegate;

    public void publish(Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override public void afterCommit() { delegate.publishEvent(event); }
                });
        } else {
            delegate.publishEvent(event);
        }
    }
}