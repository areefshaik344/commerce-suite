package com.commercesuite.common.audit.log.dto;

import com.commercesuite.common.audit.log.AuditCategory;
import com.commercesuite.common.audit.log.AuditExportFormat;
import com.commercesuite.common.audit.log.AuditSearchCriteria;
import com.commercesuite.common.audit.log.AuditSeverity;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Admin request body for {@code POST /api/v1/admin/audit/export}. */
public record AuditExportPayload(
        @NotNull AuditExportFormat format,
        UUID actorId,
        String entityType,
        String entityId,
        String action,
        AuditCategory category,
        AuditSeverity minSeverity,
        Instant from,
        Instant to,
        String requestId
) {
    public AuditSearchCriteria toCriteria() {
        return new AuditSearchCriteria(actorId, entityType, entityId, action,
                category, minSeverity, from, to, requestId);
    }
}