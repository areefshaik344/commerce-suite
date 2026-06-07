package com.commercesuite.common.audit.log;

import java.time.Instant;
import java.util.UUID;

/**
 * Filter criteria for {@link AuditSearchService}. All fields are optional;
 * a null/blank value means "no filter on this dimension".
 */
public record AuditSearchCriteria(
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
    public static AuditSearchCriteria empty() {
        return new AuditSearchCriteria(null, null, null, null, null, null, null, null, null);
    }
}