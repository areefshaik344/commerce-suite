package com.commercesuite.catalog.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record UpsertCategoryRequest(
        UUID parentId,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 140) String slug,
        @Size(max = 4000) String description,
        @Size(max = 80) String icon,
        @Min(0) Integer sortOrder,
        Boolean active
) {}