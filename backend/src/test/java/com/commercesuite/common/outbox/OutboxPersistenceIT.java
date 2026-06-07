package com.commercesuite.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class OutboxPersistenceIT extends AbstractIT {

    @Autowired OutboxService outbox;
    @Autowired OutboxEventRepository repo;

    @Test @Transactional
    void records_pending_row_with_payload() {
        var ev = outbox.record("USER", UUID.randomUUID().toString(),
                "auth.user.registered",
                Map.of("email", "x@example.com"),
                Map.of("schemaVersion", "1"),
                "corr-1");
        assertThat(ev.getId()).isNotNull();
        assertThat(ev.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(ev.getAttemptCount()).isZero();
        assertThat(ev.getNextAttemptAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(ev.getPayload()).contains("x@example.com");
        List<OutboxEvent> rows = repo.findAll();
        assertThat(rows).extracting(OutboxEvent::getId).contains(ev.getId());
    }
}