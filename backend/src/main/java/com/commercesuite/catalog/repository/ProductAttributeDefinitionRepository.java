package com.commercesuite.catalog.repository;

import com.commercesuite.catalog.entity.ProductAttributeDefinition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeDefinitionRepository extends JpaRepository<ProductAttributeDefinition, UUID> {
    Optional<ProductAttributeDefinition> findByCode(String code);
    boolean existsByCode(String code);
    List<ProductAttributeDefinition> findByCategoryIdOrderBySortOrderAsc(UUID categoryId);
}