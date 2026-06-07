package com.commercesuite.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReserveInventoryRequest(
        @NotNull @Min(1) Integer qty,
        @NotNull @Min(0) Long unitPricePaise,
        UUID cartId,
        Integer ttlSeconds) {}