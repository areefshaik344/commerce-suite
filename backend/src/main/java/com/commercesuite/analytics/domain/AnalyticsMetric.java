package com.commercesuite.analytics.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** KPI catalog row. */
@Entity
@Table(name = "analytics_metrics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AnalyticsMetric {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;
    @Column(nullable = false, unique = true, length = 96) private String code;
    @Column(name = "display_name", nullable = false, length = 160) private String displayName;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "metric_type", nullable = false, columnDefinition = "analytics_metric_type")
    private AnalyticsMetricType metricType;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "analytics_category")
    private AnalyticsCategory category;

    @Column(length = 32) private String unit;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}