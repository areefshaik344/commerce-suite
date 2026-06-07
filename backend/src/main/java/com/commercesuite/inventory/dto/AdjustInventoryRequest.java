package com.commercesuite.inventory.dto;

import com.commercesuite.inventory.entity.InventoryAdjustmentReason;
import jakarta.validation.constraints.NotNull;

public record AdjustInventoryRequest(
        @NotNull InventoryAdjustmentReason reason,
        @NotNull Integer quantityDelta,
        String notes) {}