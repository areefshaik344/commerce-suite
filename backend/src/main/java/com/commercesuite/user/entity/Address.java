package com.commercesuite.user.entity;

import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "addresses")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE addresses SET deleted_at = now(), updated_at = now(), is_default = false WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Address extends AuditableEntity {

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

    @PrePersist void defaults() {
        if (country == null) country = "IN";
        if (type    == null) type    = AddressType.HOME;
    }
}
