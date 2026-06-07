package com.commercesuite.common.audit.log;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Persistent configuration row for the {@link AuditEventRegistry}.
 * One row per canonical outbox event_type.
 */
@Entity
@Table(name = "audit_event_mappings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AuditEventMapping {

    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "event_type", nullable = false, unique = true, length = 128)
    private String eventType;

    @Column(nullable = false, length = 96) private String action;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "audit_category")
    private AuditCategory category;

    @Column(nullable = false, length = 16) private String severity;

    @Column(name = "actor_type", nullable = false, length = 32) private String actorType;

    @Column(nullable = false) private boolean enabled;

    @Column private String description;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @Version @Column(nullable = false) private long version;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (severity == null) severity = AuditSeverity.INFO.name();
    }

    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }

    public AuditSeverity severityEnum() {
        try { return AuditSeverity.valueOf(severity); }
        catch (IllegalArgumentException ex) { return AuditSeverity.INFO; }
    }

    public AuditAction actionEnum() {
        try { return AuditAction.valueOf(action); }
        catch (IllegalArgumentException ex) { return null; }
    }

    public AuditActorType actorTypeEnum() {
        try { return AuditActorType.valueOf(actorType); }
        catch (IllegalArgumentException ex) { return AuditActorType.SYSTEM; }
    }
}