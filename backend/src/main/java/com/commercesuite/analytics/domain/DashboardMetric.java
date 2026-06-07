package com.commercesuite.analytics.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Denormalised read model — latest value per (scope, scope_id, metric). */
@Entity
@Table(name = "dashboard_metrics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class DashboardMetric {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "dashboard_scope")
    private DashboardScope scope;

    @Column(name = "scope_id") @JdbcTypeCode(SqlTypes.UUID) private UUID scopeId;
    @Column(name = "metric_code", nullable = false, length = 96) private String metricCode;
    @Column(nullable = false, precision = 18, scale = 4) private BigDecimal value;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON) private String dimensions;

    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (updatedAt == null) updatedAt = Instant.now();
        if (dimensions == null) dimensions = "{}";
        if (value == null) value = BigDecimal.ZERO;
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}