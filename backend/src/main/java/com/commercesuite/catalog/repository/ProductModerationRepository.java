package com.commercesuite.catalog.repository;

import com.commercesuite.catalog.entity.ProductModeration;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductModerationRepository extends JpaRepository<ProductModeration, UUID> {
    List<ProductModeration> findByProductIdOrderByCreatedAtDesc(UUID productId);
}