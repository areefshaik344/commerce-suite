package com.commercesuite.analytics.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Append-only raw analytics fact row (see V016). */
@Entity
@Table(name = "analytics_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AnalyticsEvent {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "source_event_id", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.UUID) private UUID sourceEventId;

    @Column(name = "event_type", nullable = false, length = 128) private String eventType;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "analytics_category")
    private AnalyticsCategory category;

    @Column(name = "aggregate_type", nullable = false, length = 64) private String aggregateType;
    @Column(name = "aggregate_id",   nullable = false, length = 128) private String aggregateId;

    @Column(name = "actor_id")    @JdbcTypeCode(SqlTypes.UUID) private UUID actorId;
    @Column(name = "vendor_id")   @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;
    @Column(name = "customer_id") @JdbcTypeCode(SqlTypes.UUID) private UUID customerId;

    @Column(precision = 18, scale = 4) private BigDecimal amount;
    @Column(length = 8) private String currency;
    private Integer quantity;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON) private String dimensions;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON) private String payload;

    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "ingested_at", nullable = false, updatable = false) private Instant ingestedAt;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (ingestedAt == null) ingestedAt = Instant.now();
        if (occurredAt == null) occurredAt = ingestedAt;
        if (dimensions == null) dimensions = "{}";
        if (payload == null) payload = "{}";
    }
}