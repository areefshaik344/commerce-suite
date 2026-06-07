package com.commercesuite.payouts.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="payout_batches")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class PayoutBatch extends AuditableEntity {
  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="payout_batch_status") private PayoutBatchStatus status;
  @Column(nullable=false, length=3) private String currency;
  @Column(name="total_paise",  nullable=false) private long totalPaise;
  @Column(name="payout_count", nullable=false) private int payoutCount;
  @Column(name="generated_at", nullable=false) private Instant generatedAt;
  @Column(name="completed_at") private Instant completedAt;
  @Column(length=500) private String notes;

  @PrePersist void defaults() {
    if (status == null) status = PayoutBatchStatus.CREATED;
    if (currency == null) currency = "INR";
    if (generatedAt == null) generatedAt = Instant.now();
  }
}