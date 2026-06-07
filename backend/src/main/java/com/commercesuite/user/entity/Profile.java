package com.commercesuite.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE profiles SET deleted_at = now(), updated_at = now() WHERE user_id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Profile {
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "full_name", length = 120)    private String fullName;
    @Column(name = "display_name", length = 80)  private String displayName;
    @Column(name = "avatar_url")                  private String avatarUrl;
    @Column(length = 20)                          private String gender;
    @Column(name = "date_of_birth")               private LocalDate dateOfBirth;
    @Column(length = 280)                         private String bio;
    @Column(nullable = false, length = 10)        private String locale;

    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false)                private Instant updatedAt;
    @CreatedBy  @Column(name = "created_by", updatable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID createdBy;
    @LastModifiedBy @Column(name = "updated_by") @JdbcTypeCode(SqlTypes.UUID)               private UUID updatedBy;
    @Column(name = "deleted_at") private Instant deletedAt;

    @Version private long version;

    @PrePersist void defaults() { if (locale == null) locale = "en-IN"; }
    public boolean isDeleted() { return deletedAt != null; }
}
