package com.commercesuite.inventory.repository;

import com.commercesuite.inventory.entity.InventoryLowStockRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLowStockRuleRepository extends JpaRepository<InventoryLowStockRule, UUID> {
    Optional<InventoryLowStockRule> findByVariantId(UUID variantId);
    List<InventoryLowStockRule> findByVendorIdAndEnabledTrue(UUID vendorId);
}