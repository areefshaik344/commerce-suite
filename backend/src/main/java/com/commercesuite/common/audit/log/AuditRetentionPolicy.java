package com.commercesuite.common.audit.log;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Per-category audit retention duration. Pure policy — no purger runs here. */
@Entity
@Table(name = "audit_retention_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AuditRetentionPolicy {

    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, unique = true, columnDefinition = "audit_category")
    private AuditCategory category;

    @Column(name = "retention_days", nullable = false) private int retentionDays;

    @Column private String description;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @Version @Column(nullable = false) private long version;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}