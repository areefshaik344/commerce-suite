package com.commercesuite.catalog.dto.storefront;

import com.commercesuite.common.api.PageResponse;
import java.util.List;

/** Search response: page of cards + facets + sort metadata. */
public record ProductSearchResultDto(
        PageResponse<ProductCardDto> page,
        StorefrontFacetsDto facets,
        List<SortOptionDto> sortOptions,
        String appliedSort
) {}