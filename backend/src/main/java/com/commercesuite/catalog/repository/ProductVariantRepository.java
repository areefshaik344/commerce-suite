package com.commercesuite.catalog.repository;

import com.commercesuite.catalog.entity.ProductVariant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProductIdOrderByCreatedAtAsc(UUID productId);
    boolean existsBySku(String sku);
}