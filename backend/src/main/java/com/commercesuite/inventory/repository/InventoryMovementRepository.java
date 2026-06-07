package com.commercesuite.inventory.repository;

import com.commercesuite.inventory.entity.InventoryMovement;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
    Page<InventoryMovement> findByVariantIdOrderByCreatedAtDesc(UUID variantId, Pageable pageable);
    Page<InventoryMovement> findByVendorIdOrderByCreatedAtDesc(UUID vendorId, Pageable pageable);
}