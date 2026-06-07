package com.commercesuite.payouts.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="vendor_payouts")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class VendorPayout extends AuditableEntity {
  @Column(name="vendor_id",     nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;
  @Column(name="batch_id")      @JdbcTypeCode(SqlTypes.UUID) private UUID batchId;
  @Column(name="settlement_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID settlementId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="payout_status") private PayoutStatus status;

  @Column(nullable=false, length=3) private String currency;
  @Column(name="amount_paise", nullable=false) private long amountPaise;
  @Column(name="bank_reference", length=255) private String bankReference;
  @Column(name="gateway_provider", length=64) private String gatewayProvider;
  @Column(name="failure_code", length=64) private String failureCode;
  @Column(name="failure_message", length=500) private String failureMessage;
  @Column(name="scheduled_at") private Instant scheduledAt;
  @Column(name="processed_at") private Instant processedAt;
  @Column(name="completed_at") private Instant completedAt;

  @PrePersist void defaults() {
    if (status == null) status = PayoutStatus.CREATED;
    if (currency == null) currency = "INR";
  }
}