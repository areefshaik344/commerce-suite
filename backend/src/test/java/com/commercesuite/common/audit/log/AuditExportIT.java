package com.commercesuite.common.audit.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.audit.ActorContextHolder;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class AuditExportIT extends AbstractIT {

    @Autowired AuditExportService exporter;
    @Autowired AuditExportRequestRepository repo;
    @Autowired ActorContextHolder actors;

    @AfterEach void clear() { actors.clear(); }

    @Test @Transactional
    void records_export_request_metadata() {
        UUID admin = UUID.randomUUID();
        actors.set(new ActorContext(admin, "ADMIN", "req-x"));
        var req = exporter.request(AuditExportFormat.CSV, AuditSearchCriteria.empty());
        assertThat(req.getId()).isNotNull();
        assertThat(req.getStatus()).isEqualTo(AuditExportStatus.PENDING);
        assertThat(req.getFormat()).isEqualTo(AuditExportFormat.CSV);
        assertThat(repo.findById(req.getId())).isPresent();
    }
}