package com.commercesuite.inventory.dto;

import jakarta.validation.constraints.Size;

public record UpdateInventoryRequest(
        Boolean active,
        @Size(max = 40) String warehouseCode) {}