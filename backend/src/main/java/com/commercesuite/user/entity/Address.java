package com.commercesuite.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "addresses")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Address {
    @Id @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "address_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AddressType type;

    @Column(name = "contact_name", nullable = false, length = 80) private String contactName;
    @Column(nullable = false, length = 20)  private String phone;
    @Column(nullable = false, length = 120) private String line1;
    @Column(length = 120)                   private String line2;
    @Column(nullable = false, length = 60)  private String city;
    @Column(nullable = false, length = 60)  private String state;
    @Column(nullable = false, length = 10)  private String pincode;
    @Column(nullable = false, length = 2)   private String country;

    @Column(name = "is_default", nullable = false) private boolean isDefault;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "deleted_at") private Instant deletedAt;

    @Version private long version;

    @PrePersist void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (country == null) country = "IN";
        if (type == null) type = AddressType.HOME;
    }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
