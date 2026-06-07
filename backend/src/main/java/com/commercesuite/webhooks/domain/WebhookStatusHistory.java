package com.commercesuite.webhooks.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "webhook_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class WebhookStatusHistory {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "delivery_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID)
    private UUID deliveryId;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "from_status", columnDefinition = "webhook_delivery_status")
    private WebhookDeliveryStatus fromStatus;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "to_status", nullable = false, columnDefinition = "webhook_delivery_status")
    private WebhookDeliveryStatus toStatus;

    @Column private String reason;

    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (occurredAt == null) occurredAt = Instant.now();
    }
}