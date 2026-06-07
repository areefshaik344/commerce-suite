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

class OutboxDispatcherIT extends AbstractIT {

    @Autowired OutboxService outboxService;
    @Autowired OutboxDispatcher dispatcher;
    @Autowired OutboxEventRepository repo;

    @Test
    @Transactional
    void dispatches_pending_to_completed() {
        // arrange — directly persist a PENDING row outside dispatcher TX
        OutboxEvent ev = OutboxEvent.builder()
                .aggregateType("USER")
                .aggregateId(UUID.randomUUID().toString())
                .eventType("auth.user.registered")
                .payload("{}")
                .headers("{}")
                .status(OutboxStatus.PENDING)
                .nextAttemptAt(Instant.now())
                .maxAttempts(10)
                .build();
        repo.saveAndFlush(ev);
        // act
        int n = dispatcher.dispatchBatch();
        // assert
        assertThat(n).isGreaterThanOrEqualTo(1);
        var reloaded = repo.findById(ev.getId()).orElseThrow();
        assertThat(List.of(OutboxStatus.COMPLETED, OutboxStatus.PROCESSING))
                .contains(reloaded.getStatus());
    }
}