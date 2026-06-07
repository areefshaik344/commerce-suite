package com.commercesuite.catalog.repository;

import com.commercesuite.catalog.entity.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Category> findByParentIdOrderBySortOrderAscNameAsc(UUID parentId);
    List<Category> findAllByOrderBySortOrderAscNameAsc();
    boolean existsByParentId(UUID parentId);
}