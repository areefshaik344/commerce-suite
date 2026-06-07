package com.commercesuite.payments.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/** External gateway pointer attached to intents/attempts/transactions. */
@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentGatewayReference {
  @Column(name="gateway_provider", length=64)  private String provider;
  @Column(name="gateway_reference", length=255) private String reference;
}