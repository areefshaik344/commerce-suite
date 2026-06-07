package com.commercesuite.payouts.dto;
import com.commercesuite.payouts.entity.PayoutBatch;
import com.commercesuite.payouts.entity.PayoutBatchStatus;
import java.time.Instant;
import java.util.UUID;

public record PayoutBatchDto(UUID id, PayoutBatchStatus status, String currency,
                             long totalPaise, int payoutCount, Instant generatedAt,
                             Instant completedAt, String notes) {
  public static PayoutBatchDto from(PayoutBatch b) {
    return new PayoutBatchDto(b.getId(), b.getStatus(), b.getCurrency(), b.getTotalPaise(),
        b.getPayoutCount(), b.getGeneratedAt(), b.getCompletedAt(), b.getNotes());
  }
}