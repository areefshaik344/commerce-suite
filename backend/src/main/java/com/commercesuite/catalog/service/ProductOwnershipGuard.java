package com.commercesuite.catalog.service;

import com.commercesuite.catalog.entity.Product;
import com.commercesuite.catalog.repository.ProductRepository;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.vendor.entity.Vendor;
import com.commercesuite.vendor.repository.VendorRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalog ownership guard.
 *
 * <p>Rules:
 *   - Vendors may only touch products belonging to their own vendor row.
 *   - Admins (MODERATE_PRODUCTS) and SUPER_ADMIN bypass ownership.
 */
@Component
@RequiredArgsConstructor
public class ProductOwnershipGuard {

    private final ProductRepository productRepo;
    private final VendorRepository vendorRepo;

    @Transactional(readOnly = true)
    public Vendor requireVendorFor(UUID userId) {
        return vendorRepo.findByUserId(userId).orElseThrow(() -> AppException.notFound("Vendor"));
    }

    @Transactional(readOnly = true)
    public Product requireOwned(UUID productId, ActorContext actor) {
        Product p = productRepo.findById(productId).orElseThrow(() -> AppException.notFound("Product"));
        if (isAdmin(actor)) return p;
        Vendor v = requireVendorFor(actor.userId());
        if (!p.getVendorId().equals(v.getId()))
            throw AppException.forbidden("Not your product");
        return p;
    }

    public boolean isAdmin(ActorContext a) {
        return a.hasPermission(Permissions.MODERATE_PRODUCTS)
                || a.hasRole(AppRole.ADMIN.name())
                || a.hasRole(AppRole.SUPER_ADMIN.name());
    }
}