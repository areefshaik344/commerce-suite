package com.commercesuite.common.audit.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.common.outbox.OutboxDispatchEvent;
import com.commercesuite.common.outbox.OutboxEvent;
import com.commercesuite.common.outbox.OutboxStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Drives the consumer directly with a synthetic outbox dispatch event. */
class AuditConsumerIT extends AbstractIT {

    @Autowired AuditLogRepository repo;
    @Autowired AuditConsumer consumer;

    @Test @Transactional
    void mapped_event_produces_audit_record() {
        OutboxEvent e = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("USER").aggregateId(UUID.randomUUID().toString())
                .eventType("auth.user_logged_in")
                .payload("{}").headers("{\"actorId\":\"" + UUID.randomUUID() + "\"}")
                .status(OutboxStatus.COMPLETED)
                .nextAttemptAt(Instant.now())
                .build();
        long before = repo.count();
        consumer.onDispatched(new OutboxDispatchEvent(e));
        assertThat(repo.count()).isEqualTo(before + 1);
    }

    @Test @Transactional
    void unmapped_event_is_skipped() {
        OutboxEvent e = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("X").aggregateId("y")
                .eventType("totally.unknown.event")
                .payload("{}").headers("{}")
                .status(OutboxStatus.COMPLETED)
                .nextAttemptAt(Instant.now())
                .build();
        long before = repo.count();
        consumer.onDispatched(new OutboxDispatchEvent(e));
        assertThat(repo.count()).isEqualTo(before);
    }
}