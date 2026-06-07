package com.commercesuite.payments.dto;
import com.commercesuite.payments.entity.PaymentTransaction;
import com.commercesuite.payments.entity.PaymentTransactionType;
import java.time.Instant;
import java.util.UUID;

public record PaymentTransactionDto(UUID id, UUID intentId, PaymentTransactionType txType,
                                    long amountPaise, String currency, String gatewayProvider,
                                    String gatewayReference, UUID parentTxId, Instant occurredAt) {
  public static PaymentTransactionDto from(PaymentTransaction t) {
    return new PaymentTransactionDto(t.getId(), t.getIntentId(), t.getTxType(),
        t.getAmountPaise(), t.getCurrency(), t.getGatewayProvider(), t.getGatewayReference(),
        t.getParentTxId(), t.getOccurredAt());
  }
}