package com.commercesuite.common.audit.log;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Append-only — see V013 (REVOKE UPDATE, DELETE). */
@Entity
@Table(name = "audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AuditLog {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "actor_id") @JdbcTypeCode(SqlTypes.UUID) private UUID actorId;

    @Column(name = "actor_type", nullable = false, length = 32) private String actorType;

    @Column(name = "entity_type", nullable = false, length = 64) private String entityType;
    @Column(name = "entity_id", length = 128) private String entityId;

    @Column(nullable = false, length = 96) private String action;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "audit_severity")
    private AuditSeverity severity;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON) private String metadata;

    @Column(name = "request_id", length = 64) private String requestId;
    @Column(name = "correlation_id", length = 128) private String correlationId;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "user_agent", length = 255) private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (severity == null) severity = AuditSeverity.INFO;
        if (metadata == null) metadata = "{}";
    }
}