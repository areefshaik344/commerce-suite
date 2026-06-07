package com.commercesuite.analytics.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Append-only snapshot of a KPI value at a moment in time. */
@Entity
@Table(name = "analytics_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AnalyticsSnapshot {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;
    @Column(name = "metric_code", nullable = false, length = 96) private String metricCode;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "dashboard_scope")
    private DashboardScope scope;

    @Column(name = "scope_id") @JdbcTypeCode(SqlTypes.UUID) private UUID scopeId;
    @Column(nullable = false, precision = 18, scale = 4) private BigDecimal value;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON) private String dimensions;

    @Column(name = "captured_at", nullable = false, updatable = false) private Instant capturedAt;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (capturedAt == null) capturedAt = Instant.now();
        if (dimensions == null) dimensions = "{}";
    }
}