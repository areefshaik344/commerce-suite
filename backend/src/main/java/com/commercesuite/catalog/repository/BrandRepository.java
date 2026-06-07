package com.commercesuite.catalog.repository;

import com.commercesuite.catalog.entity.Brand;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
    Optional<Brand> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Brand> findAllByActiveTrueOrderByNameAsc();
}