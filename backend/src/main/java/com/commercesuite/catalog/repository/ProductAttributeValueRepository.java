package com.commercesuite.catalog.repository;

import com.commercesuite.catalog.entity.ProductAttributeValue;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, UUID> {
    List<ProductAttributeValue> findByProductId(UUID productId);
}