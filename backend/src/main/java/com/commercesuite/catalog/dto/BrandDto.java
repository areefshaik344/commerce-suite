package com.commercesuite.catalog.dto;

import com.commercesuite.catalog.entity.Brand;
import java.util.UUID;

public record BrandDto(UUID id, String name, String slug, String description, String logoUrl, boolean active) {
    public static BrandDto from(Brand b) {
        return new BrandDto(b.getId(), b.getName(), b.getSlug(), b.getDescription(), b.getLogoUrl(), b.isActive());
    }
}