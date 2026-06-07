package com.commercesuite.payouts.dto;
import com.commercesuite.payouts.entity.PayoutStatus;
import com.commercesuite.payouts.entity.VendorPayout;
import java.time.Instant;
import java.util.UUID;

public record VendorPayoutDto(UUID id, UUID vendorId, UUID batchId, UUID settlementId,
                              PayoutStatus status, String currency, long amountPaise,
                              String bankReference, String gatewayProvider,
                              String failureCode, String failureMessage,
                              Instant scheduledAt, Instant processedAt, Instant completedAt) {
  public static VendorPayoutDto from(VendorPayout p) {
    return new VendorPayoutDto(p.getId(), p.getVendorId(), p.getBatchId(), p.getSettlementId(),
        p.getStatus(), p.getCurrency(), p.getAmountPaise(), p.getBankReference(),
        p.getGatewayProvider(), p.getFailureCode(), p.getFailureMessage(),
        p.getScheduledAt(), p.getProcessedAt(), p.getCompletedAt());
  }
}