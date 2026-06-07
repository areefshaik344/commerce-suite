package com.commercesuite.webhooks.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "webhook_subscriptions",
       uniqueConstraints = @UniqueConstraint(name = "uq_webhook_sub",
                                             columnNames = {"endpoint_id","event_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class WebhookSubscription {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "endpoint_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID)
    private UUID endpointId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(nullable = false) private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}