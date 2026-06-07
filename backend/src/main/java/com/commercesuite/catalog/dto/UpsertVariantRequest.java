package com.commercesuite.catalog.dto;

import jakarta.validation.constraints.*;

public record UpsertVariantRequest(
        @NotBlank @Size(max = 80) String sku,
        @Size(max = 80) String barcode,
        @Min(0) long pricePaise,
        @Min(0) Long compareAtPaise,
        Integer weightGrams,
        Integer lengthMm,
        Integer widthMm,
        Integer heightMm,
        String optionsJson,
        Boolean isDefault,
        Boolean active
) {}