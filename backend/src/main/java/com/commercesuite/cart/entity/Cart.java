package com.commercesuite.cart.entity;

import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "carts")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE carts SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Cart extends AuditableEntity {

    @Column(name = "user_id") @JdbcTypeCode(SqlTypes.UUID) private UUID userId;
    @Column(name = "guest_token", length = 64) private String guestToken;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "cart_status")
    private CartStatus status;

    @Column(nullable = false, length = 3) private String currency;

    @Column(name = "merged_into_id") @JdbcTypeCode(SqlTypes.UUID) private UUID mergedIntoId;
    @Column(name = "last_activity_at", nullable = false) private Instant lastActivityAt;

    @PrePersist void defaults() {
        if (status == null) status = CartStatus.ACTIVE;
        if (currency == null) currency = "INR";
        if (lastActivityAt == null) lastActivityAt = Instant.now();
    }
}