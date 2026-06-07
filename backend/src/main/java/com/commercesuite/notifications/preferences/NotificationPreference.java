package com.commercesuite.notifications.preferences;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_preferences",
    uniqueConstraints = @UniqueConstraint(name = "uq_notifpref",
        columnNames = {"user_id","channel","category"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class NotificationPreference {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "user_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID)
    private UUID userId;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notification_channel")
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notification_category")
    private NotificationCategory category;

    @Column(nullable = false) private boolean enabled;

    @Column(name = "marketing_opt_in", nullable = false) private boolean marketingOptIn;

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