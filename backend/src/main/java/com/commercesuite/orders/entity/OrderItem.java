package com.commercesuite.orders.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="order_items") @Getter @Setter @NoArgsConstructor @SuperBuilder
@SQLDelete(sql="UPDATE order_items SET deleted_at=now(), updated_at=now() WHERE id=? AND version=?")
@SQLRestriction("deleted_at IS NULL")
public class OrderItem extends AuditableEntity {
  @Column(name="order_id",        nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID orderId;
  @Column(name="vendor_order_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorOrderId;
  @Column(name="vendor_id",       nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;
  @Column(name="product_id",      nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID productId;
  @Column(name="variant_id",      nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID variantId;
  @Column(name="reservation_id") @JdbcTypeCode(SqlTypes.UUID) private UUID reservationId;
  @Column(length=128) private String sku;
  @Column(nullable=false) private int qty;
  @Column(name="unit_price_paise",    nullable=false) private long unitPricePaise;
  @Column(name="line_subtotal_paise", nullable=false) private long lineSubtotalPaise;
  @Column(name="line_discount_paise", nullable=false) private long lineDiscountPaise;
  @Column(name="line_tax_paise",      nullable=false) private long lineTaxPaise;
  @Column(name="line_total_paise",    nullable=false) private long lineTotalPaise;
  @Column(name="cancelled_qty",  nullable=false) private int cancelledQty;
  @Column(name="returned_qty",   nullable=false) private int returnedQty;
  @Column(name="refunded_paise", nullable=false) private long refundedPaise;
  @Column(name="product_snapshot", nullable=false, columnDefinition="jsonb") @JdbcTypeCode(SqlTypes.JSON)
  private String productSnapshot;
  @Column(name="shipment_id") @JdbcTypeCode(SqlTypes.UUID) private UUID shipmentId;
  @Column(nullable=false, length=32) private String status;

  @PrePersist void defaults() { if (status == null) status = "ACTIVE"; }
}
