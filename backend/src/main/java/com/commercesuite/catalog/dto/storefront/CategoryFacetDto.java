package com.commercesuite.catalog.dto.storefront;

import java.util.UUID;

public record CategoryFacetDto(UUID id, String name, String slug, long productCount) {}