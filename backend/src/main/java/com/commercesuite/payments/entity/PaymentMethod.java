package com.commercesuite.payments.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="payment_methods")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class PaymentMethod extends AuditableEntity {
  @Column(name="customer_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID customerId;

  @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable=false, columnDefinition="payment_method_kind") private PaymentMethodKind kind;

  @Column(length=64) private String brand;
  @Column(length=8)  private String last4;
  @Column(name="display_label", length=128) private String displayLabel;
  @Column(name="gateway_token", length=255) private String gatewayToken;
  @Column(name="is_default", nullable=false) private boolean isDefault;
}