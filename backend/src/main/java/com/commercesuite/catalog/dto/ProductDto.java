package com.commercesuite.catalog.dto;

import com.commercesuite.catalog.entity.Product;
import com.commercesuite.catalog.entity.ProductStatus;
import java.time.Instant;
import java.util.UUID;

public record ProductDto(
        UUID id, UUID vendorId, UUID categoryId, UUID brandId,
        String slug, String title, String shortDescription, String description,
        ProductStatus status, String statusReason,
        Instant submittedAt, Instant approvedAt, Instant rejectedAt, Instant suspendedAt, Instant archivedAt,
        Instant createdAt, Instant updatedAt) {
    public static ProductDto from(Product p) {
        return new ProductDto(p.getId(), p.getVendorId(), p.getCategoryId(), p.getBrandId(),
                p.getSlug(), p.getTitle(), p.getShortDescription(), p.getDescription(),
                p.getStatus(), p.getStatusReason(),
                p.getSubmittedAt(), p.getApprovedAt(), p.getRejectedAt(),
                p.getSuspendedAt(), p.getArchivedAt(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}