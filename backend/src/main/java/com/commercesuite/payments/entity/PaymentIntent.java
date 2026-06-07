package com.commercesuite.payments.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Financial record — append-only (REVOKE DELETE in V012). */
@Entity @Table(name="payment_intents")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class PaymentIntent extends AuditableEntity {
  @Column(name="customer_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID customerId;
  @Column(name="order_id") @JdbcTypeCode(SqlTypes.UUID) private UUID orderId;
  @Column(name="checkout_id") @JdbcTypeCode(SqlTypes.UUID) private UUID checkoutId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="payment_status") private PaymentStatus status;

  @Column(nullable=false, length=3) private String currency;

  @Column(name="amount_paise",     nullable=false) private long amountPaise;
  @Column(name="authorized_paise", nullable=false) private long authorizedPaise;
  @Column(name="captured_paise",   nullable=false) private long capturedPaise;
  @Column(name="refunded_paise",   nullable=false) private long refundedPaise;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name="method_kind", columnDefinition="payment_method_kind") private PaymentMethodKind methodKind;

  @Column(name="payment_method_id") @JdbcTypeCode(SqlTypes.UUID) private UUID paymentMethodId;

  @Column(name="idempotency_key", nullable=false, length=128) private String idempotencyKey;
  @Column(name="gateway_provider", length=64) private String gatewayProvider;
  @Column(name="gateway_intent_id", length=255) private String gatewayIntentId;

  @Column(name="failure_code", length=64) private String failureCode;
  @Column(name="failure_message", length=500) private String failureMessage;

  @Column(nullable=false, columnDefinition="jsonb") @JdbcTypeCode(SqlTypes.JSON)
  private String metadata;

  @Column(name="authorized_at") private Instant authorizedAt;
  @Column(name="captured_at")   private Instant capturedAt;
  @Column(name="failed_at")     private Instant failedAt;
  @Column(name="cancelled_at")  private Instant cancelledAt;

  @PrePersist void defaults() {
    if (status == null) status = PaymentStatus.CREATED;
    if (currency == null) currency = "INR";
    if (metadata == null) metadata = "{}";
  }

  /** Remaining refundable balance per MONEY_SPEC §4. */
  public long refundableRemainingPaise() {
    return Math.max(0L, capturedPaise - refundedPaise);
  }
}