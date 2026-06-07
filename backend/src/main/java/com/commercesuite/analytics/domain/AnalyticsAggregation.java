package com.commercesuite.analytics.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "analytics_aggregations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AnalyticsAggregation {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "metric_code", nullable = false, length = 96) private String metricCode;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "analytics_period")
    private AnalyticsPeriod period;

    @Column(name = "bucket_start", nullable = false) private Instant bucketStart;
    @Column(name = "bucket_end",   nullable = false) private Instant bucketEnd;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "dashboard_scope")
    private DashboardScope scope;

    @Column(name = "scope_id") @JdbcTypeCode(SqlTypes.UUID) private UUID scopeId;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON) private String dimensions;

    @Column(name = "value_count", nullable = false) private long valueCount;
    @Column(name = "value_sum",   nullable = false, precision = 18, scale = 4) private BigDecimal valueSum;
    @Column(name = "value_min", precision = 18, scale = 4) private BigDecimal valueMin;
    @Column(name = "value_max", precision = 18, scale = 4) private BigDecimal valueMax;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (dimensions == null) dimensions = "{}";
        if (valueSum == null) valueSum = BigDecimal.ZERO;
        if (scope == null) scope = DashboardScope.ADMIN;
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}