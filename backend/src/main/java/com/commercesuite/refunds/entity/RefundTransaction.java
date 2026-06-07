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

@Entity @Table(name="refund_transactions") @Getter @Setter @NoArgsConstructor @SuperBuilder
@SQLDelete(sql="UPDATE refund_transactions SET deleted_at=now(), updated_at=now() WHERE id=? AND version=?")
@SQLRestriction("deleted_at IS NULL")
public class RefundTransaction extends AuditableEntity {
  @Column(name="refund_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID refundId;
  @Column(name="amount_paise", nullable=false) private long amountPaise;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="refund_status") private RefundStatus status;

  @Column(name="gateway_ref", length=255) private String gatewayRef;
  @Column(name="processed_at") private Instant processedAt;

  @PrePersist void defaults() { if (status == null) status = RefundStatus.PENDING; }
}
