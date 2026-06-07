package com.commercesuite.inventory;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.catalog.service.BrandService;
import com.commercesuite.catalog.service.CategoryService;
import com.commercesuite.catalog.service.ProductService;
import com.commercesuite.catalog.service.ProductVariantService;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.inventory.dto.AdjustInventoryRequest;
import com.commercesuite.inventory.entity.InventoryAdjustmentReason;
import com.commercesuite.inventory.service.InventoryAdjustmentService;
import com.commercesuite.inventory.service.InventoryService;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.vendor.service.VendorAdminService;
import com.commercesuite.vendor.service.VendorApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryOwnershipIT extends AbstractIT {

    @Autowired AuthService auth;
    @Autowired RoleService roles;
    @Autowired VendorApplicationService apps;
    @Autowired VendorAdminService vendorAdmin;
    @Autowired CategoryService categories;
    @Autowired BrandService brands;
    @Autowired ProductService products;
    @Autowired ProductVariantService variants;
    @Autowired InventoryService inventoryService;
    @Autowired InventoryAdjustmentService adjustmentService;

    @Test
    void otherVendorCannotAdjustInventory() {
        var v1 = InventoryTestSupport.vendorWithVariant(auth, roles, apps, vendorAdmin,
                categories, brands, products, variants, "own1");
        var v2 = InventoryTestSupport.vendorWithVariant(auth, roles, apps, vendorAdmin,
                categories, brands, products, variants, "own2");

        var a1 = InventoryTestSupport.actorOf(v1.vendor(), AppRole.VENDOR);
        var a2 = InventoryTestSupport.actorOf(v2.vendor(), AppRole.VENDOR);

        inventoryService.ensureInventoryFor(v1.variantId(), a1);

        assertThatThrownBy(() -> adjustmentService.adjust(v1.variantId(),
                new AdjustInventoryRequest(InventoryAdjustmentReason.INCREASE, 5, "x"), a2))
                .isInstanceOf(AppException.class);
    }
}