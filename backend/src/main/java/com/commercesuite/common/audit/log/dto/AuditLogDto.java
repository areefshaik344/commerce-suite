package com.commercesuite.common.audit.log.dto;

import com.commercesuite.common.audit.log.AuditCategory;
import com.commercesuite.common.audit.log.AuditLog;
import com.commercesuite.common.audit.log.AuditSeverity;
import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        UUID actorId,
        String actorType,
        String entityType,
        String entityId,
        String action,
        AuditCategory category,
        AuditSeverity severity,
        String metadata,
        String requestId,
        String correlationId,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {
    public static AuditLogDto from(AuditLog a) {
        return new AuditLogDto(
                a.getId(), a.getActorId(), a.getActorType(),
                a.getEntityType(), a.getEntityId(),
                a.getAction(), a.getCategory(), a.getSeverity(),
                a.getMetadata(), a.getRequestId(), a.getCorrelationId(),
                a.getIpAddress(), a.getUserAgent(), a.getCreatedAt());
    }
}