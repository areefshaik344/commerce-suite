package com.commercesuite.payments.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Immutable ledger entry. */
@Entity @Table(name="payment_transactions")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class PaymentTransaction extends AuditableEntity {
  @Column(name="intent_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID intentId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name="tx_type", nullable=false, columnDefinition="payment_tx_type")
  private PaymentTransactionType txType;

  @Column(name="amount_paise", nullable=false) private long amountPaise;
  @Column(nullable=false, length=3) private String currency;

  @Column(name="gateway_provider", length=64) private String gatewayProvider;
  @Column(name="gateway_reference", length=255) private String gatewayReference;
  @Column(name="parent_tx_id") @JdbcTypeCode(SqlTypes.UUID) private UUID parentTxId;

  @Column(nullable=false, columnDefinition="jsonb") @JdbcTypeCode(SqlTypes.JSON)
  private String metadata;

  @Column(name="occurred_at", nullable=false) private Instant occurredAt;

  @PrePersist void defaults() {
    if (currency == null) currency = "INR";
    if (metadata == null) metadata = "{}";
    if (occurredAt == null) occurredAt = Instant.now();
  }
}