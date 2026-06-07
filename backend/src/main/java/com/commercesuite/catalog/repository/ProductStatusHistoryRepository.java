package com.commercesuite.catalog.repository;

import com.commercesuite.catalog.entity.ProductStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStatusHistoryRepository extends JpaRepository<ProductStatusHistory, UUID> {
    List<ProductStatusHistory> findByProductIdOrderByChangedAtDesc(UUID productId);
}