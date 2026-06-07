package com.commercesuite.catalog.repository;

import com.commercesuite.catalog.entity.ProductReview;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {
    Page<ProductReview> findByProductId(UUID productId, Pageable pageable);
    Optional<ProductReview> findByProductIdAndCustomerId(UUID productId, UUID customerId);
    boolean existsByProductIdAndCustomerId(UUID productId, UUID customerId);
    long countByProductId(UUID productId);
}