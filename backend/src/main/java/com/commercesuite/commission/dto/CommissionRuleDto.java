package com.commercesuite.commission.dto;
import com.commercesuite.commission.entity.CommissionRule;
import com.commercesuite.commission.entity.CommissionScope;
import com.commercesuite.commission.entity.CommissionType;
import java.time.Instant;
import java.util.UUID;

public record CommissionRuleDto(UUID id, CommissionScope scope, UUID vendorId, UUID categoryId,
                                CommissionType ruleType, Integer percentBps, Long fixedPaise,
                                String tiersJson, long minFeePaise, Long maxFeePaise,
                                Instant effectiveFrom, Instant effectiveTo, boolean active) {
  public static CommissionRuleDto from(CommissionRule r) {
    return new CommissionRuleDto(r.getId(), r.getScope(), r.getVendorId(), r.getCategoryId(),
        r.getRuleType(), r.getPercentBps(), r.getFixedPaise(), r.getTiersJson(),
        r.getMinFeePaise(), r.getMaxFeePaise(), r.getEffectiveFrom(), r.getEffectiveTo(), r.isActive());
  }
}