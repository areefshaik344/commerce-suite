package com.commercesuite.catalog.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record UpdateProductRequest(
        UUID categoryId,
        UUID brandId,
        @Size(max = 200) String title,
        @Size(max = 500) String shortDescription,
        @Size(max = 20000) String description
) {}