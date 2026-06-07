package com.commercesuite.settlement.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="settlement_lines")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class SettlementLine extends AuditableEntity {
  @Column(name="settlement_id",  nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID settlementId;
  @Column(name="vendor_order_id",nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorOrderId;

  @Column(name="gross_paise",        nullable=false) private long grossPaise;
  @Column(name="refund_paise",       nullable=false) private long refundPaise;
  @Column(name="commission_paise",   nullable=false) private long commissionPaise;
  @Column(name="platform_fee_paise", nullable=false) private long platformFeePaise;
  @Column(name="net_paise",          nullable=false) private long netPaise;

  @Column(nullable=false, columnDefinition="jsonb") @JdbcTypeCode(SqlTypes.JSON)
  private String metadata;

  @PrePersist void defaults() { if (metadata == null) metadata = "{}"; }
}