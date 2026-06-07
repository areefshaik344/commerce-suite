package com.commercesuite.inventory.repository;

import com.commercesuite.inventory.entity.InventoryAdjustment;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, UUID> {
    Page<InventoryAdjustment> findByVariantIdOrderByCreatedAtDesc(UUID variantId, Pageable pageable);
    Page<InventoryAdjustment> findByVendorIdOrderByCreatedAtDesc(UUID vendorId, Pageable pageable);
}