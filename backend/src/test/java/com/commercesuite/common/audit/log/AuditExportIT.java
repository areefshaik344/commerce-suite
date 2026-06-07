package com.commercesuite.common.audit.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.common.audit.ActorContext;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import static org.mockito.Mockito.when;

class AuditExportIT extends AbstractIT {

    @Autowired AuditExportService exporter;
    @Autowired AuditExportRequestRepository repo;
    @MockBean ActorContextHolder actors;

    @BeforeEach void stub() {
        when(actors.current()).thenReturn(new ActorContext(
                UUID.randomUUID(), Set.of("ADMIN"), Set.of(), "req-x"));
    }

    @Test @Transactional
    void records_export_request_metadata() {
        var req = exporter.request(AuditExportFormat.CSV, AuditSearchCriteria.empty());
        assertThat(req.getId()).isNotNull();
        assertThat(req.getStatus()).isEqualTo(AuditExportStatus.PENDING);
        assertThat(req.getFormat()).isEqualTo(AuditExportFormat.CSV);
        assertThat(repo.findById(req.getId())).isPresent();
    }
}