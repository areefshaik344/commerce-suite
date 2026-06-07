package com.commercesuite.catalog.dto.storefront;

import java.util.List;

/** Facet payload for product search; counts span the full unpaginated result set. */
public record StorefrontFacetsDto(
        List<BrandFilterDto> brands,
        List<CategoryFacetDto> categories,
        PriceRangeDto priceRange,
        List<RatingBucketDto> ratings
) {}