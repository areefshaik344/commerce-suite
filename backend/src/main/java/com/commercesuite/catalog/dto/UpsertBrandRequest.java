package com.commercesuite.catalog.dto;

import jakarta.validation.constraints.*;

public record UpsertBrandRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 140) String slug,
        @Size(max = 4000) String description,
        @Size(max = 500) String logoUrl,
        Boolean active
) {}