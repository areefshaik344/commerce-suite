package com.commercesuite.commission.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen snapshot of a commission calculation per vendor_order. */
@Entity @Table(name="commission_calculations")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class CommissionCalculation extends AuditableEntity {
  @Column(name="vendor_order_id", nullable=false, unique=true) @JdbcTypeCode(SqlTypes.UUID)
  private UUID vendorOrderId;
  @Column(name="vendor_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;
  @Column(name="rule_id")   @JdbcTypeCode(SqlTypes.UUID) private UUID ruleId;

  @Column(name="rule_snapshot", nullable=false, columnDefinition="jsonb") @JdbcTypeCode(SqlTypes.JSON)
  private String ruleSnapshot;

  @Column(name="taxable_paise",       nullable=false) private long taxablePaise;
  @Column(name="commission_paise",    nullable=false) private long commissionPaise;
  @Column(name="platform_fee_paise",  nullable=false) private long platformFeePaise;

  @Column(name="calculated_at", nullable=false) private Instant calculatedAt;

  @PrePersist void defaults() { if (calculatedAt == null) calculatedAt = Instant.now(); }
}