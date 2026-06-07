package com.commercesuite.catalog.service;

import com.commercesuite.catalog.dto.CreateReviewRequest;
import com.commercesuite.catalog.dto.ProductReviewDto;
import com.commercesuite.catalog.entity.Product;
import com.commercesuite.catalog.entity.ProductReview;
import com.commercesuite.catalog.entity.ProductReviewStatus;
import com.commercesuite.catalog.entity.ProductStatus;
import com.commercesuite.catalog.event.CatalogEvents.ProductReviewCreatedEvent;
import com.commercesuite.catalog.repository.ProductRepository;
import com.commercesuite.catalog.repository.ProductReviewRepository;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository reviewRepo;
    private final ProductRepository productRepo;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public ProductReviewDto create(UUID productId, UUID customerId, CreateReviewRequest r) {
        Product p = productRepo.findById(productId).orElseThrow(() -> AppException.notFound("Product"));
        if (p.getStatus() != ProductStatus.APPROVED)
            throw AppException.conflict(ErrorCode.CONFLICT, "Cannot review a product that is not APPROVED");
        if (reviewRepo.existsByProductIdAndCustomerId(productId, customerId))
            throw AppException.conflict(ErrorCode.CONFLICT, "You already reviewed this product");

        ProductReview review = reviewRepo.save(ProductReview.builder()
                .productId(productId).customerId(customerId)
                .rating((short) r.rating()).title(r.title()).reviewText(r.reviewText())
                .verifiedPurchase(false)
                .status(ProductReviewStatus.PUBLISHED)
                .helpfulCount(0).build());

        events.publishEvent(new ProductReviewCreatedEvent(review.getId(), productId, customerId,
                r.rating(), Instant.now(clock)));
        return ProductReviewDto.from(review);
    }

    @Transactional
    public void delete(UUID reviewId, UUID customerId) {
        ProductReview existing = reviewRepo.findById(reviewId).orElseThrow(() -> AppException.notFound("Review"));
        if (!existing.getCustomerId().equals(customerId))
            throw AppException.forbidden("Not your review");
        reviewRepo.delete(existing); // soft delete via @SQLDelete
    }

    @Transactional(readOnly = true)
    public Page<ProductReviewDto> list(UUID productId, Pageable pageable) {
        return reviewRepo.findByProductId(productId, pageable).map(ProductReviewDto::from);
    }
}