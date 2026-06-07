package com.commercesuite.common.audit.log;

import java.util.Map;
import java.util.UUID;

/** Immutable record carried into AuditService.record(...). */
public record AuditContext(
        UUID actorId,
        AuditActorType actorType,
        String entityType,
        String entityId,
        AuditAction action,
        AuditSeverity severity,
        AuditCategory category,
        Map<String, Object> metadata,
        String requestId,
        String correlationId,
        String ipAddress,
        String userAgent
) {
    public static AuditContext of(AuditActorType actorType, AuditAction action,
                                  String entityType, String entityId) {
        return new AuditContext(null, actorType, entityType, entityId, action,
                AuditSeverity.INFO, AuditCategory.SYSTEM, Map.of(),
                null, null, null, null);
    }
}