package com.commercesuite.catalog.dto;

import jakarta.validation.constraints.*;

public record CreateReviewRequest(
        @Min(1) @Max(5) int rating,
        @Size(max = 160) String title,
        @Size(max = 4000) String reviewText
) {}