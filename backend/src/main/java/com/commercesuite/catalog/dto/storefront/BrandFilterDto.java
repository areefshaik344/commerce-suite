package com.commercesuite.catalog.dto.storefront;

import java.util.UUID;

/** Brand entry suitable for filter chips: id, name, slug, optional logo, product count. */
public record BrandFilterDto(UUID id, String name, String slug, String logoUrl, long productCount) {}