package com.commercesuite.commission.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="commission_rules")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class CommissionRule extends AuditableEntity {

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="commission_scope") private CommissionScope scope;

  @Column(name="vendor_id")   @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;
  @Column(name="category_id") @JdbcTypeCode(SqlTypes.UUID) private UUID categoryId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name="rule_type", nullable=false, columnDefinition="commission_type") private CommissionType ruleType;

  @Column(name="percent_bps") private Integer percentBps;
  @Column(name="fixed_paise") private Long fixedPaise;

  @Column(name="tiers_json", columnDefinition="jsonb") @JdbcTypeCode(SqlTypes.JSON)
  private String tiersJson;

  @Column(name="min_fee_paise", nullable=false) private long minFeePaise;
  @Column(name="max_fee_paise") private Long maxFeePaise;

  @Column(name="effective_from", nullable=false) private Instant effectiveFrom;
  @Column(name="effective_to") private Instant effectiveTo;
  @Column(nullable=false) private boolean active;

  @PrePersist void defaults() {
    if (scope == null) scope = CommissionScope.GLOBAL;
    if (effectiveFrom == null) effectiveFrom = Instant.now();
  }
}