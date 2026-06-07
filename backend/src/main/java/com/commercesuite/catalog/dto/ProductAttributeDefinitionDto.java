package com.commercesuite.catalog.dto;

import com.commercesuite.catalog.entity.ProductAttributeDataType;
import com.commercesuite.catalog.entity.ProductAttributeDefinition;
import java.util.UUID;

public record ProductAttributeDefinitionDto(
        UUID id, UUID categoryId, String code, String label,
        ProductAttributeDataType dataType, boolean required, boolean filterable,
        String unit, String enumOptions, int sortOrder, boolean active) {
    public static ProductAttributeDefinitionDto from(ProductAttributeDefinition d) {
        return new ProductAttributeDefinitionDto(d.getId(), d.getCategoryId(), d.getCode(), d.getLabel(),
                d.getDataType(), d.isRequired(), d.isFilterable(),
                d.getUnit(), d.getEnumOptions(), d.getSortOrder(), d.isActive());
    }
}