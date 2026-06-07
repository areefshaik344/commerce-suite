package com.commercesuite.analytics.service;

import com.commercesuite.analytics.domain.AnalyticsEvent;
import com.commercesuite.analytics.event.AnalyticsEvents;
import com.commercesuite.analytics.repository.AnalyticsEventRepository;
import com.commercesuite.common.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists raw analytics events. Runs in {@code REQUIRES_NEW} so a
 * failure here NEVER bubbles into the dispatcher transaction.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsEventRepository repo;
    private final OutboxPublisher outbox;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalyticsEvent record(AnalyticsEvent event) {
        if (event.getSourceEventId() != null) {
            var existing = repo.findBySourceEventId(event.getSourceEventId());
            if (existing.isPresent()) return existing.get();
        }
        AnalyticsEvent saved = repo.save(event);
        outbox.publish(AnalyticsEvents.AGGREGATE, saved.getId().toString(),
                AnalyticsEvents.EVENT_RECORDED,
                new AnalyticsEvents.EventRecordedPayload(
                        saved.getId(), saved.getSourceEventId(),
                        saved.getEventType(), saved.getCategory(),
                        saved.getOccurredAt()));
        return saved;
    }
}