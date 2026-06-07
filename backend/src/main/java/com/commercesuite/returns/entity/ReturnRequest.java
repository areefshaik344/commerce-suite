package com.commercesuite.returns.entity;
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

@Entity @Table(name="return_requests") @Getter @Setter @NoArgsConstructor @SuperBuilder
@SQLDelete(sql="UPDATE return_requests SET deleted_at=now(), updated_at=now() WHERE id=? AND version=?")
@SQLRestriction("deleted_at IS NULL")
public class ReturnRequest extends AuditableEntity {
  @Column(name="order_id",        nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID orderId;
  @Column(name="vendor_order_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorOrderId;
  @Column(name="vendor_id",       nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;
  @Column(name="customer_id",     nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID customerId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="return_status") private ReturnStatus status;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="return_reason") private ReturnReason reason;

  @Column(length=1000) private String note;
  @Column(name="pickup_address_id") @JdbcTypeCode(SqlTypes.UUID) private UUID pickupAddressId;
  @Column(name="refund_paise", nullable=false) private long refundPaise;
  @Column(name="requested_at", nullable=false) private Instant requestedAt;
  @Column(name="resolved_at")  private Instant resolvedAt;
  @Column(name="received_at")  private Instant receivedAt;

  @PrePersist void defaults() {
    if (status == null) status = ReturnStatus.REQUESTED;
    if (requestedAt == null) requestedAt = Instant.now();
  }
}
