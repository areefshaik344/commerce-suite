package com.commercesuite.payments.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="payment_status_history")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class PaymentStatusHistory extends AuditableEntity {
  @Column(name="intent_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID intentId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name="from_status", columnDefinition="payment_status") private PaymentStatus fromStatus;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name="to_status", nullable=false, columnDefinition="payment_status") private PaymentStatus toStatus;

  @Column(name="actor_id") @JdbcTypeCode(SqlTypes.UUID) private UUID actorId;
  @Column(name="actor_role", length=32) private String actorRole;
  @Column(length=500) private String reason;
  @Column(name="changed_at", nullable=false) private Instant changedAt;

  @PrePersist void defaults() { if (changedAt == null) changedAt = Instant.now(); }
}