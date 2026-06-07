package com.commercesuite.shipping.entity;
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

@Entity @Table(name="shipments") @Getter @Setter @NoArgsConstructor @SuperBuilder
@SQLDelete(sql="UPDATE shipments SET deleted_at=now(), updated_at=now() WHERE id=? AND version=?")
@SQLRestriction("deleted_at IS NULL")
public class Shipment extends AuditableEntity {
  @Column(name="order_id",        nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID orderId;
  @Column(name="vendor_order_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorOrderId;
  @Column(name="vendor_id",       nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="shipment_status") private ShipmentStatus status;

  @Column(length=120) private String carrier;
  @Column(name="tracking_number", length=120) private String trackingNumber;
  @Column(name="shipping_method", length=40) private String shippingMethod;
  @Column(name="shipping_paise", nullable=false) private long shippingPaise;
  @Column(name="estimated_delivery_at") private Instant estimatedDeliveryAt;
  @Column(name="shipped_at")   private Instant shippedAt;
  @Column(name="delivered_at") private Instant deliveredAt;

  @PrePersist void defaults() { if (status == null) status = ShipmentStatus.CREATED; }
}
