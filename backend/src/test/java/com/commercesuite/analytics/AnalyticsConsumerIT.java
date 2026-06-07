package com.commercesuite.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.analytics.consumer.AnalyticsConsumer;
import com.commercesuite.analytics.repository.AnalyticsEventRepository;
import com.commercesuite.common.outbox.OutboxDispatchEvent;
import com.commercesuite.common.outbox.OutboxEvent;
import com.commercesuite.common.outbox.OutboxStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Drives the {@link AnalyticsConsumer} directly with synthetic outbox
 * dispatch events. Verifies (a) classification + persistence, (b) that
 * unmapped events are ignored, and (c) duplicate ingestion is idempotent.
 */
class AnalyticsConsumerIT extends AbstractIT {

    @Autowired AnalyticsConsumer consumer;
    @Autowired AnalyticsEventRepository events;

    @Test
    void classified_event_is_ingested() {
        UUID id = UUID.randomUUID();
        OutboxEvent o = OutboxEvent.builder()
                .id(id)
                .aggregateType("ORDER").aggregateId(UUID.randomUUID().toString())
                .eventType("order.created")
                .payload("{\"total\":1234.56,\"currency\":\"INR\"}")
                .headers("{}")
                .status(OutboxStatus.COMPLETED)
                .nextAttemptAt(Instant.now())
                .publishedAt(Instant.now())
                .build();
        consumer.on(new OutboxDispatchEvent(o));
        assertThat(events.findBySourceEventId(id)).isPresent();
    }

    @Test
    void unmapped_event_is_skipped() {
        OutboxEvent o = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("X").aggregateId("y")
                .eventType("totally.unknown.event")
                .payload("{}").headers("{}")
                .status(OutboxStatus.COMPLETED)
                .nextAttemptAt(Instant.now())
                .build();
        long before = events.count();
        consumer.on(new OutboxDispatchEvent(o));
        assertThat(events.count()).isEqualTo(before);
    }

    @Test
    void duplicate_source_event_is_idempotent() {
        UUID id = UUID.randomUUID();
        OutboxEvent o = OutboxEvent.builder()
                .id(id)
                .aggregateType("ORDER").aggregateId(UUID.randomUUID().toString())
                .eventType("order.created")
                .payload("{}").headers("{}")
                .status(OutboxStatus.COMPLETED)
                .nextAttemptAt(Instant.now())
                .build();
        consumer.on(new OutboxDispatchEvent(o));
        long after1 = events.count();
        consumer.on(new OutboxDispatchEvent(o));
        assertThat(events.count()).isEqualTo(after1);
    }
}