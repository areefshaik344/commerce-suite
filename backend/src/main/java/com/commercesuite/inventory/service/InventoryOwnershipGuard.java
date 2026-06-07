package com.commercesuite.inventory.service;

import com.commercesuite.catalog.entity.Product;
import com.commercesuite.catalog.entity.ProductVariant;
import com.commercesuite.catalog.repository.ProductRepository;
import com.commercesuite.catalog.repository.ProductVariantRepository;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.inventory.entity.InventoryItem;
import com.commercesuite.inventory.repository.InventoryItemRepository;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.vendor.entity.Vendor;
import com.commercesuite.vendor.repository.VendorRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inventory ownership guard.
 *
 * Rules:
 *  - Vendors may only touch inventory for variants belonging to their own products.
 *  - Admins (MODERATE_PRODUCTS or ADMIN/SUPER_ADMIN role) bypass ownership.
 */
@Component
@RequiredArgsConstructor
public class InventoryOwnershipGuard {

    private final VendorRepository vendorRepo;
    private final ProductRepository productRepo;
    private final ProductVariantRepository variantRepo;
    private final InventoryItemRepository inventoryRepo;

    public boolean isAdmin(ActorContext a) {
        return a.hasPermission(Permissions.MODERATE_PRODUCTS)
                || a.hasRole(AppRole.ADMIN.name())
                || a.hasRole(AppRole.SUPER_ADMIN.name());
    }

    @Transactional(readOnly = true)
    public Vendor requireVendorFor(UUID userId) {
        return vendorRepo.findByUserId(userId).orElseThrow(() -> AppException.notFound("Vendor"));
    }

    /** Resolve the variant and ensure the actor may manage its inventory. Returns variant + owning vendorId. */
    @Transactional(readOnly = true)
    public OwnedVariant requireOwnedVariant(UUID variantId, ActorContext actor) {
        ProductVariant v = variantRepo.findById(variantId)
                .orElseThrow(() -> AppException.notFound("Variant"));
        Product p = productRepo.findById(v.getProductId())
                .orElseThrow(() -> AppException.notFound("Product"));
        if (isAdmin(actor)) return new OwnedVariant(v, p.getVendorId());
        Vendor vendor = requireVendorFor(actor.userId());
        if (!p.getVendorId().equals(vendor.getId()))
            throw AppException.forbidden("Not your variant");
        return new OwnedVariant(v, vendor.getId());
    }

    @Transactional(readOnly = true)
    public InventoryItem requireOwnedItem(UUID variantId, ActorContext actor) {
        OwnedVariant ov = requireOwnedVariant(variantId, actor);
        return inventoryRepo.findByVariantId(ov.variant().getId())
                .orElseThrow(() -> AppException.notFound("InventoryItem"));
    }

    public record OwnedVariant(ProductVariant variant, UUID vendorId) {}
}