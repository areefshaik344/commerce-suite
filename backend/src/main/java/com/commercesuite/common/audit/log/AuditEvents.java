package com.commercesuite.common.audit.log;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain events emitted by the audit module itself. All events flow
 * through the durable {@link com.commercesuite.common.outbox.OutboxPublisher}.
 */
public final class AuditEvents {
    private AuditEvents() {}

    public static final String AGGREGATE = "AUDIT_LOG";

    public static final String RECORD_CREATED      = "audit.record_created";
    public static final String EXPORT_REQUESTED    = "audit.export_requested";
    public static final String COVERAGE_WARNING    = "audit.coverage_warning";

    public record RecordCreatedPayload(
            UUID auditId, UUID actorId,
            String entityType, String entityId,
            String action, AuditCategory category, AuditSeverity severity,
            Instant occurredAt) {}

    public record ExportRequestedPayload(
            UUID exportId, UUID requestedBy, String format,
            Map<String, Object> criteria, Instant requestedAt) {}

    public record CoverageWarningPayload(
            String reason, Map<String, Object> details, Instant detectedAt) {}
}