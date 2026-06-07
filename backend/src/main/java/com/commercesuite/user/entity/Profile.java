package com.commercesuite.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "profiles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Profile {
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "full_name", length = 120)    private String fullName;
    @Column(name = "display_name", length = 80)  private String displayName;
    @Column(name = "avatar_url")                  private String avatarUrl;
    @Column(length = 20)                          private String gender;
    @Column(name = "date_of_birth")               private LocalDate dateOfBirth;
    @Column(length = 280)                         private String bio;
    @Column(nullable = false, length = 10)        private String locale;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    @PrePersist void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (locale == null) locale = "en-IN";
    }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
