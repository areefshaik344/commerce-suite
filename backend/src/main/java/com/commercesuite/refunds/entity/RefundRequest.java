package com.commercesuite.refunds.entity;
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

@Entity @Table(name="refund_requests") @Getter @Setter @NoArgsConstructor @SuperBuilder
@SQLDelete(sql="UPDATE refund_requests SET deleted_at=now(), updated_at=now() WHERE id=? AND version=?")
@SQLRestriction("deleted_at IS NULL")
public class RefundRequest extends AuditableEntity {
  @Column(name="order_id",        nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID orderId;
  @Column(name="vendor_order_id") @JdbcTypeCode(SqlTypes.UUID) private UUID vendorOrderId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name="source_type", nullable=false, columnDefinition="refund_source_type")
  private RefundSourceType sourceType;

  @Column(name="source_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID sourceId;
  @Column(name="amount_paise", nullable=false) private long amountPaise;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="refund_status") private RefundStatus status;

  @Column(length=500) private String reason;
  @Column(name="requested_at", nullable=false) private Instant requestedAt;
  @Column(name="completed_at") private Instant completedAt;

  @PrePersist void defaults() {
    if (status == null) status = RefundStatus.PENDING;
    if (requestedAt == null) requestedAt = Instant.now();
  }
}
