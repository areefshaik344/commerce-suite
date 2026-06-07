package com.commercesuite.settlement.dto;
import com.commercesuite.settlement.entity.SettlementLine;
import java.util.UUID;

public record SettlementLineDto(UUID id, UUID vendorOrderId, long grossPaise, long refundPaise,
                                long commissionPaise, long platformFeePaise, long netPaise) {
  public static SettlementLineDto from(SettlementLine l) {
    return new SettlementLineDto(l.getId(), l.getVendorOrderId(), l.getGrossPaise(), l.getRefundPaise(),
        l.getCommissionPaise(), l.getPlatformFeePaise(), l.getNetPaise());
  }
}