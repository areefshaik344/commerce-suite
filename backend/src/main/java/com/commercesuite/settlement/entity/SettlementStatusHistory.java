package com.commercesuite.settlement.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="settlement_status_history")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class SettlementStatusHistory extends AuditableEntity {
  @Column(name="settlement_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID settlementId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name="from_status", columnDefinition="settlement_status") private SettlementStatus fromStatus;
  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name="to_status", nullable=false, columnDefinition="settlement_status") private SettlementStatus toStatus;

  @Column(name="actor_id") @JdbcTypeCode(SqlTypes.UUID) private UUID actorId;
  @Column(name="actor_role", length=32) private String actorRole;
  @Column(length=500) private String reason;
  @Column(name="changed_at", nullable=false) private Instant changedAt;

  @PrePersist void defaults() { if (changedAt == null) changedAt = Instant.now(); }
}