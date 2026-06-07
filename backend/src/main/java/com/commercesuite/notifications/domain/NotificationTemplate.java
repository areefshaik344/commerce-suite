package com.commercesuite.notifications.domain;

import com.commercesuite.notifications.preferences.NotificationCategory;
import com.commercesuite.notifications.preferences.NotificationChannel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class NotificationTemplate {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(nullable = false, length = 96) private String code;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notification_category")
    private NotificationCategory category;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notification_channel")
    private NotificationChannel channel;

    @Column(nullable = false, length = 16) private String locale;
    @Column(nullable = false) private int version;

    @Column(name = "title_template", nullable = false) private String titleTemplate;
    @Column(name = "body_template", nullable = false)  private String bodyTemplate;
    @Column(name = "action_url_template")              private String actionUrlTemplate;

    @Column(nullable = false) private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "deleted_at") private Instant deletedAt;

    @Version @Column(name = "optimistic_version", nullable = false) private long optimisticVersion;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (locale == null) locale = "en";
        if (version == 0) version = 1;
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}