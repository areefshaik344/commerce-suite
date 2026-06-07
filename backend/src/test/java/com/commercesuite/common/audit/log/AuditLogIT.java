package com.commercesuite.common.audit.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class AuditLogIT extends AbstractIT {
    @Autowired AuditService audit;
    @Autowired AuditLogRepository repo;

    @Test @Transactional
    void records_append_only_row() {
        var saved = audit.record(new AuditContext(
                UUID.randomUUID(), AuditActorType.USER,
                "USER", UUID.randomUUID().toString(),
                AuditAction.USER_LOGGED_IN, AuditSeverity.INFO,
                Map.of("ip", "127.0.0.1"), "req-1", "corr-1",
                "127.0.0.1", "JUnit"));
        assertThat(saved.getId()).isNotNull();
        assertThat(repo.findById(saved.getId())).isPresent();
    }
}