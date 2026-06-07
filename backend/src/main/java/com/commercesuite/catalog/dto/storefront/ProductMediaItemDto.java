package com.commercesuite.catalog.dto.storefront;

import java.util.UUID;

public record ProductMediaItemDto(UUID id, String url, String altText, String mediaType, int sortOrder) {}