package com.commercesuite.orders.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Financial record — append-only. NO @SQLDelete (BLOCKER B-05). */
@Entity @Table(name="orders") @Getter @Setter @NoArgsConstructor @SuperBuilder
public class Order extends AuditableEntity {
  @Column(name="customer_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID customerId;
  @Column(name="checkout_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID checkoutId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="order_status") private OrderStatus status;

  @Column(nullable=false, length=3) private String currency;

  @Column(name="subtotal_paise",        nullable=false) private long subtotalPaise;
  @Column(name="discount_paise",        nullable=false) private long discountPaise;
  @Column(name="coupon_discount_paise", nullable=false) private long couponDiscountPaise;
  @Column(name="shipping_paise",        nullable=false) private long shippingPaise;
  @Column(name="tax_paise",             nullable=false) private long taxPaise;
  @Column(name="platform_fee_paise",    nullable=false) private long platformFeePaise;
  @Column(name="grand_total_paise",     nullable=false) private long grandTotalPaise;

  @Column(name="coupon_code", length=64) private String couponCode;

  @Column(name="address_snapshot", nullable=false, columnDefinition="jsonb") @JdbcTypeCode(SqlTypes.JSON)
  private String addressSnapshot;

  @Column(name="pricing_snapshot", nullable=false, columnDefinition="jsonb") @JdbcTypeCode(SqlTypes.JSON)
  private String pricingSnapshot;

  @Column(name="placed_at", nullable=false) private Instant placedAt;
  @Column(name="cancelled_at") private Instant cancelledAt;
  @Column(name="delivered_at") private Instant deliveredAt;

  @PrePersist void defaults() {
    if (status == null) status = OrderStatus.CREATED;
    if (currency == null) currency = "INR";
    if (placedAt == null) placedAt = Instant.now();
  }
}
