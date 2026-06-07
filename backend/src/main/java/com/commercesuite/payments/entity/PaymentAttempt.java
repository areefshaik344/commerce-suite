package com.commercesuite.payments.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="payment_attempts")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class PaymentAttempt extends AuditableEntity {
  @Column(name="intent_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID intentId;
  @Column(name="attempt_number", nullable=false) private int attemptNumber;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="payment_status") private PaymentStatus status;

  @Column(name="gateway_provider", length=64) private String gatewayProvider;
  @Column(name="gateway_reference", length=255) private String gatewayReference;

  @Column(name="request_payload",  nullable=false, columnDefinition="jsonb") @JdbcTypeCode(SqlTypes.JSON)
  private String requestPayload;
  @Column(name="response_payload", nullable=false, columnDefinition="jsonb") @JdbcTypeCode(SqlTypes.JSON)
  private String responsePayload;

  @Column(name="failure_code", length=64) private String failureCode;
  @Column(name="failure_message", length=500) private String failureMessage;
  @Column(name="attempted_at", nullable=false) private Instant attemptedAt;

  @PrePersist void defaults() {
    if (attemptedAt == null) attemptedAt = Instant.now();
    if (requestPayload == null) requestPayload = "{}";
    if (responsePayload == null) responsePayload = "{}";
  }
}