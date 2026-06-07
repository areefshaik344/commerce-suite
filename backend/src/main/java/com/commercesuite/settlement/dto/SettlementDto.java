package com.commercesuite.settlement.dto;
import com.commercesuite.settlement.entity.Settlement;
import com.commercesuite.settlement.entity.SettlementStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SettlementDto(UUID id, UUID vendorId, SettlementStatus status, String currency,
                            Instant periodStart, Instant periodEnd,
                            long grossPaise, long refundPaise, long commissionPaise,
                            long platformFeePaise, long adjustmentPaise, long netPayablePaise,
                            String calculationHash, Instant lockedAt, Instant paidAt, UUID payoutId,
                            List<SettlementLineDto> lines) {
  public static SettlementDto from(Settlement s, List<SettlementLineDto> lines) {
    return new SettlementDto(s.getId(), s.getVendorId(), s.getStatus(), s.getCurrency(),
        s.getPeriodStart(), s.getPeriodEnd(), s.getGrossPaise(), s.getRefundPaise(),
        s.getCommissionPaise(), s.getPlatformFeePaise(), s.getAdjustmentPaise(),
        s.getNetPayablePaise(), s.getCalculationHash(), s.getLockedAt(), s.getPaidAt(),
        s.getPayoutId(), lines);
  }
}