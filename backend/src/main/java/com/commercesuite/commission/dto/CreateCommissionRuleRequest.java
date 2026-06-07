package com.commercesuite.commission.dto;
import com.commercesuite.commission.entity.CommissionScope;
import com.commercesuite.commission.entity.CommissionType;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record CreateCommissionRuleRequest(
    @NotNull CommissionScope scope,
    UUID vendorId,
    UUID categoryId,
    @NotNull CommissionType ruleType,
    @Min(0) @Max(10000) Integer percentBps,
    @Min(0) Long fixedPaise,
    String tiersJson,
    @Min(0) Long minFeePaise,
    @Min(0) Long maxFeePaise,
    Instant effectiveFrom,
    Instant effectiveTo
) {}