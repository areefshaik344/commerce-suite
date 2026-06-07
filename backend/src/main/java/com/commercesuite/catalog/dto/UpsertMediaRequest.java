package com.commercesuite.catalog.dto;

import com.commercesuite.catalog.entity.ProductMediaType;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record UpsertMediaRequest(
        UUID variantId,
        @NotBlank @Size(max = 2000) String url,
        @NotNull ProductMediaType mediaType,
        @Size(max = 200) String altText,
        @Min(0) Integer sortOrder
) {}