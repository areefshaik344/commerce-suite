package com.commercesuite.common.audit.log;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only export request. Tracks intent + status only — actual file
 * generation is deferred to a future phase.
 */
@Entity
@Table(name = "audit_export_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AuditExportRequest {

    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "requested_by", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID requestedBy;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "audit_export_format")
    private AuditExportFormat format;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "audit_export_status")
    private AuditExportStatus status;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String criteria;

    @Column(name = "row_count") private Integer rowCount;
    @Column(name = "file_ref", length = 512) private String fileRef;
    @Column(name = "error_message") private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "completed_at") private Instant completedAt;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = AuditExportStatus.PENDING;
        if (criteria == null) criteria = "{}";
    }
}