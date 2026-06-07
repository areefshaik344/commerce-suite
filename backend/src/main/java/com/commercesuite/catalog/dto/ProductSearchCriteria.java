package com.commercesuite.catalog.dto;

import com.commercesuite.catalog.entity.ProductStatus;
import java.util.UUID;

/** Backend-ready search criteria (keyword + filters). */
public record ProductSearchCriteria(
        String keyword,
        UUID categoryId,
        UUID brandId,
        UUID vendorId,
        ProductStatus status
) {}