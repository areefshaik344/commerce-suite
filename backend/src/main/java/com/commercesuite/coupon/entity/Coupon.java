package com.commercesuite.coupon.entity;

import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "coupons")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE coupons SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Coupon extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 64) private String code;
    @Column(length = 160) private String label;
    @Column(columnDefinition = "text") private String description;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "coupon_type")
    private CouponType type;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "coupon_scope")
    private CouponScope scope;

    @Column(name = "percent_off", precision = 5, scale = 2) private BigDecimal percentOff;
    @Column(name = "amount_off_paise")  private Long amountOffPaise;
    @Column(name = "max_discount_paise") private Long maxDiscountPaise;
    @Column(name = "min_order_paise", nullable = false) private long minOrderPaise;
    @Column(nullable = false, length = 3) private String currency;

    @Column(name = "vendor_id")   @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;
    @Column(name = "category_id") @JdbcTypeCode(SqlTypes.UUID) private UUID categoryId;

    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at",   nullable = false) private Instant endsAt;

    @Column(name = "usage_limit_total")    private Integer usageLimitTotal;
    @Column(name = "usage_limit_per_user") private Integer usageLimitPerUser;

    @Column(nullable = false) private boolean active;

    @PrePersist void defaults() {
        if (currency == null) currency = "INR";
        if (scope == null) scope = CouponScope.GLOBAL;
    }
}