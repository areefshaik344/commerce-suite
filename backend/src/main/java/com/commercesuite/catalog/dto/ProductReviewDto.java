package com.commercesuite.catalog.dto;

import com.commercesuite.catalog.entity.ProductReview;
import com.commercesuite.catalog.entity.ProductReviewStatus;
import java.time.Instant;
import java.util.UUID;

public record ProductReviewDto(UUID id, UUID productId, UUID customerId,
                                int rating, String title, String reviewText,
                                boolean verifiedPurchase, ProductReviewStatus status,
                                int helpfulCount, Instant createdAt) {
    public static ProductReviewDto from(ProductReview r) {
        return new ProductReviewDto(r.getId(), r.getProductId(), r.getCustomerId(),
                r.getRating(), r.getTitle(), r.getReviewText(),
                r.isVerifiedPurchase(), r.getStatus(), r.getHelpfulCount(), r.getCreatedAt());
    }
}