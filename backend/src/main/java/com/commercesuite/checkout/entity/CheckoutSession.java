package com.commercesuite.checkout.entity;

import com.commercesuite.common.entity.AuditableEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "checkout_sessions")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE checkout_sessions SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class CheckoutSession extends AuditableEntity {

    @Column(name = "user_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID userId;
    @Column(name = "cart_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID cartId;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "checkout_status")
    private CheckoutStatus status;

    @Column(nullable = false, length = 3) private String currency;

    @Column(name = "address_id") @JdbcTypeCode(SqlTypes.UUID) private UUID addressId;

    @Column(name = "address_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String addressSnapshot;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "shipping_method", columnDefinition = "shipping_method_kind")
    private ShippingMethodKind shippingMethod;

    @Column(name = "shipping_amount_paise") private Long shippingAmountPaise;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_method", columnDefinition = "payment_method_kind")
    private PaymentMethodKind paymentMethod;

    @Column(name = "coupon_code", length = 64) private String couponCode;

    @Column(name = "subtotal_paise",        nullable = false) private long subtotalPaise;
    @Column(name = "discount_paise",        nullable = false) private long discountPaise;
    @Column(name = "coupon_discount_paise", nullable = false) private long couponDiscountPaise;
    @Column(name = "tax_paise",             nullable = false) private long taxPaise;
    @Column(name = "platform_fee_paise",    nullable = false) private long platformFeePaise;
    @Column(name = "grand_total_paise",     nullable = false) private long grandTotalPaise;

    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "idempotency_key", length = 128) private String idempotencyKey;

    @PrePersist void defaults() {
        if (status == null) status = CheckoutStatus.CREATED;
        if (currency == null) currency = "INR";
    }
}