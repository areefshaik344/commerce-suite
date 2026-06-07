package com.commercesuite.catalog.dto;

import com.commercesuite.catalog.entity.Category;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CategoryDto(UUID id, UUID parentId, String name, String slug, String description,
                          String icon, int sortOrder, boolean active, List<CategoryDto> children) {
    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getParentId(), c.getName(), c.getSlug(),
                c.getDescription(), c.getIcon(), c.getSortOrder(), c.isActive(), new ArrayList<>());
    }
}