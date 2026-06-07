package com.commercesuite.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "email_verified_at") private Instant emailVerifiedAt;
    @Column(name = "phone_verified_at") private Instant phoneVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, columnDefinition = "account_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountStatus accountStatus;

    @Column(name = "status_reason") private String statusReason;

    @Column(name = "failed_login_count", nullable = false) private int failedLoginCount;
    @Column(name = "locked_until") private Instant lockedUntil;
    @Column(name = "last_login_at") private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "deleted_at") private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false) private long version;

    @PrePersist void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (accountStatus == null) accountStatus = AccountStatus.PENDING_VERIFICATION;
    }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
