package com.commercesuite.common.audit.log.dto;

import com.commercesuite.common.audit.log.AuditExportFormat;
import com.commercesuite.common.audit.log.AuditExportRequest;
import com.commercesuite.common.audit.log.AuditExportStatus;
import java.time.Instant;
import java.util.UUID;

public record AuditExportRequestDto(
        UUID id,
        UUID requestedBy,
        AuditExportFormat format,
        AuditExportStatus status,
        String criteria,
        Integer rowCount,
        String fileRef,
        Instant createdAt,
        Instant completedAt
) {
    public static AuditExportRequestDto from(AuditExportRequest r) {
        return new AuditExportRequestDto(
                r.getId(), r.getRequestedBy(), r.getFormat(), r.getStatus(),
                r.getCriteria(), r.getRowCount(), r.getFileRef(),
                r.getCreatedAt(), r.getCompletedAt());
    }
}