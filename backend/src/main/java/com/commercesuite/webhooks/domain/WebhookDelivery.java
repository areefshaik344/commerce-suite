package com.commercesuite.webhooks.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "webhook_deliveries",
       uniqueConstraints = @UniqueConstraint(name = "uq_webhook_delivery",
                                             columnNames = {"subscription_id","source_event_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class WebhookDelivery {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "subscription_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID)
    private UUID subscriptionId;

    @Column(name = "endpoint_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID)
    private UUID endpointId;

    @Column(name = "source_event_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID)
    private UUID sourceEventId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(nullable = false, columnDefinition = "jsonb") @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "webhook_delivery_status")
    private WebhookDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "max_attempts",  nullable = false) private int maxAttempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "last_error") private String lastError;
    @Column(name = "last_response_code") private Integer lastResponseCode;
    @Column(name = "delivered_at") private Instant deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = WebhookDeliveryStatus.PENDING;
        if (maxAttempts == 0) maxAttempts = 10;
        if (nextAttemptAt == null) nextAttemptAt = now;
        if (payload == null) payload = "{}";
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}