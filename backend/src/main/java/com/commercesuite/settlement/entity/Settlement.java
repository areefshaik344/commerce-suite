package com.commercesuite.settlement.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="settlements")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class Settlement extends AuditableEntity {
  @Column(name="vendor_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="settlement_status") private SettlementStatus status;

  @Column(nullable=false, length=3) private String currency;
  @Column(name="period_start", nullable=false) private Instant periodStart;
  @Column(name="period_end",   nullable=false) private Instant periodEnd;

  @Column(name="gross_paise",        nullable=false) private long grossPaise;
  @Column(name="refund_paise",       nullable=false) private long refundPaise;
  @Column(name="commission_paise",   nullable=false) private long commissionPaise;
  @Column(name="platform_fee_paise", nullable=false) private long platformFeePaise;
  @Column(name="adjustment_paise",   nullable=false) private long adjustmentPaise;
  @Column(name="net_payable_paise",  nullable=false) private long netPayablePaise;

  @Column(name="calculation_hash", length=64) private String calculationHash;
  @Column(name="locked_at") private Instant lockedAt;
  @Column(name="paid_at")   private Instant paidAt;
  @Column(name="payout_id") @JdbcTypeCode(SqlTypes.UUID) private UUID payoutId;

  @PrePersist void defaults() {
    if (status == null) status = SettlementStatus.PENDING;
    if (currency == null) currency = "INR";
  }
}