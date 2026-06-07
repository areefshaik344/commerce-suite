package com.commercesuite.coupon.entity;

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
@Table(name = "coupon_usage")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE coupon_usage SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class CouponUsage extends AuditableEntity {

    @Column(name = "coupon_id",   nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID couponId;
    @Column(name = "user_id",     nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID userId;
    @Column(name = "checkout_id") @JdbcTypeCode(SqlTypes.UUID) private UUID checkoutId;
    @Column(name = "order_id")    @JdbcTypeCode(SqlTypes.UUID) private UUID orderId;
    @Column(name = "discount_paise", nullable = false) private long discountPaise;
    @Column(name = "applied_at", nullable = false) private Instant appliedAt;
    @Column(nullable = false) private boolean committed;

    @PrePersist void defaults() {
        if (appliedAt == null) appliedAt = Instant.now();
    }
}