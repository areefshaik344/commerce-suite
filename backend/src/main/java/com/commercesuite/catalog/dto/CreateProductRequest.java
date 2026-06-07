package com.commercesuite.catalog.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreateProductRequest(
        @NotNull UUID categoryId,
        UUID brandId,
        @Size(max = 180) String slug,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 500) String shortDescription,
        @Size(max = 20000) String description
) {}