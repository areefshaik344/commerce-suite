package com.commercesuite.shipping.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="tracking_events") @Getter @Setter @NoArgsConstructor @SuperBuilder
@SQLDelete(sql="UPDATE tracking_events SET deleted_at=now(), updated_at=now() WHERE id=? AND version=?")
@SQLRestriction("deleted_at IS NULL")
public class TrackingEvent extends AuditableEntity {
  @Column(name="shipment_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID shipmentId;
  @Column(name="event_type",  nullable=false, length=64) private String eventType;
  @Column(length=500) private String description;
  @Column(length=255) private String location;
  @Column(name="occurred_at", nullable=false) private Instant occurredAt;

  @PrePersist void defaults() { if (occurredAt == null) occurredAt = Instant.now(); }
}
