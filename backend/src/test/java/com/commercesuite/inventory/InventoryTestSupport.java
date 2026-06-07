package com.commercesuite.inventory;

import com.commercesuite.auth.service.AuthService;
import com.commercesuite.catalog.CatalogTestSupport;
import com.commercesuite.catalog.dto.CreateProductRequest;
import com.commercesuite.catalog.dto.UpsertVariantRequest;
import com.commercesuite.catalog.service.BrandService;
import com.commercesuite.catalog.service.CategoryService;
import com.commercesuite.catalog.service.ProductService;
import com.commercesuite.catalog.service.ProductVariantService;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.inventory.entity.InventoryItem;
import com.commercesuite.inventory.repository.InventoryItemRepository;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.PermissionCatalog;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.user.repository.UserRepository;
import com.commercesuite.vendor.VendorTestSupport.TestUser;
import com.commercesuite.vendor.service.VendorAdminService;
import com.commercesuite.vendor.service.VendorApplicationService;
import java.util.Set;
import java.util.UUID;

/** Helpers for inventory ITs — sets up an approved vendor + product + variant + inventory item. */
public final class InventoryTestSupport {
    private InventoryTestSupport() {}

    public record VendorVariant(TestUser vendor, UUID productId, UUID variantId, UUID vendorId) {}

    public static VendorVariant vendorWithVariant(AuthService auth, RoleService roles,
                                                  VendorApplicationService apps, VendorAdminService vendorAdmin,
                                                  CategoryService categories, BrandService brands,
                                                  ProductService products, ProductVariantService variants,
                                                  String prefix) {
        var vendor = CatalogTestSupport.approvedVendor(auth, roles, apps, vendorAdmin, prefix);
        var catId  = CatalogTestSupport.newCategory(categories, "Cat-" + System.nanoTime());
        var actor  = actorOf(vendor, AppRole.VENDOR);

        var product = products.create(actor, new CreateProductRequest(catId, null, null,
                "Widget-" + System.nanoTime(), "short", "desc"));
        var variant = variants.create(actor, product.id(),
                new UpsertVariantRequest("SKU-" + System.nanoTime(), null, 19900L, null,
                        500, 100, 80, 40, "{}", true, true));
        return new VendorVariant(vendor, product.id(), variant.id(),
                /* vendorId resolved later by tests via repository if needed */ null);
    }

    public static ActorContext actorOf(TestUser u, AppRole role, PermissionCatalog catalog) {
        return new ActorContext(u.userId(), Set.of(role.name()),
                catalog.permissionsFor(role), "test-req");
    }

    /** Convenience: builds an ActorContext carrying only role-appropriate permissions. */
    public static ActorContext actorOf(TestUser u, AppRole role) {
        Set<String> perms = switch (role) {
            case VENDOR -> Set.of("MANAGE_INVENTORY","MANAGE_PRODUCTS","MANAGE_VENDOR_PROFILE");
            case ADMIN, SUPER_ADMIN -> Set.of("MODERATE_PRODUCTS","MANAGE_USERS","MANAGE_VENDORS");
            default -> Set.of();
        };
        return new ActorContext(u.userId(), Set.of(role.name()), perms, "test-req");
    }

    public static InventoryItem seedInventory(InventoryItemRepository repo,
                                              UUID variantId, UUID vendorId, int onHand) {
        return repo.save(InventoryItem.builder()
                .variantId(variantId).vendorId(vendorId)
                .onHandQty(onHand).reservedQty(0).active(true).build());
    }

    public static UUID resolveVendorId(UserRepository users, UUID userId,
                                       com.commercesuite.vendor.repository.VendorRepository vendors) {
        return vendors.findByUserId(userId).orElseThrow().getId();
    }
}