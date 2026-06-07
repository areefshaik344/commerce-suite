package com.commercesuite.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpsertLowStockRuleRequest(
        @NotNull @Min(0) Integer threshold,
        @NotNull Boolean enabled) {}