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

@Entity @Table(name="vendor_orders") @Getter @Setter @NoArgsConstructor @SuperBuilder
@SQLDelete(sql="UPDATE vendor_orders SET deleted_at=now(), updated_at=now() WHERE id=? AND version=?")
@SQLRestriction("deleted_at IS NULL")
public class VendorOrder extends AuditableEntity {
  @Column(name="order_id",  nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID orderId;
  @Column(name="vendor_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="vendor_order_status") private VendorOrderStatus status;

  @Column(name="subtotal_paise", nullable=false) private long subtotalPaise;
  @Column(name="discount_paise", nullable=false) private long discountPaise;
  @Column(name="shipping_paise", nullable=false) private long shippingPaise;
  @Column(name="tax_paise",      nullable=false) private long taxPaise;
  @Column(name="total_paise",    nullable=false) private long totalPaise;

  @Column(name="vendor_snapshot", nullable=false, columnDefinition="jsonb") @JdbcTypeCode(SqlTypes.JSON)
  private String vendorSnapshot;

  @PrePersist void defaults() { if (status == null) status = VendorOrderStatus.CREATED; }
}
