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
import com.commercesuite.inventory.repository.InventoryItemRepository;
import com.commercesuite.inventory.service.InventoryAdjustmentService;
import com.commercesuite.inventory.service.InventoryService;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.vendor.service.VendorAdminService;
import com.commercesuite.vendor.service.VendorApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryAdjustmentIT extends AbstractIT {

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
    @Autowired InventoryItemRepository itemRepo;

    @Test
    void increaseThenDecrease() {
        var vv = InventoryTestSupport.vendorWithVariant(auth, roles, apps, vendorAdmin,
                categories, brands, products, variants, "adj");
        var actor = InventoryTestSupport.actorOf(vv.vendor(), AppRole.VENDOR);
        inventoryService.ensureInventoryFor(vv.variantId(), actor);
        adjustmentService.adjust(vv.variantId(),
                new AdjustInventoryRequest(InventoryAdjustmentReason.INCREASE, 50, "in"), actor);
        adjustmentService.adjust(vv.variantId(),
                new AdjustInventoryRequest(InventoryAdjustmentReason.DECREASE, -20, "out"), actor);
        assertThat(itemRepo.findByVariantId(vv.variantId()).orElseThrow().getOnHandQty()).isEqualTo(30);
    }

    @Test
    void cannotMakeOnHandNegative() {
        var vv = InventoryTestSupport.vendorWithVariant(auth, roles, apps, vendorAdmin,
                categories, brands, products, variants, "neg");
        var actor = InventoryTestSupport.actorOf(vv.vendor(), AppRole.VENDOR);
        inventoryService.ensureInventoryFor(vv.variantId(), actor);
        assertThatThrownBy(() -> adjustmentService.adjust(vv.variantId(),
                new AdjustInventoryRequest(InventoryAdjustmentReason.DECREASE, -5, "bad"), actor))
                .isInstanceOf(AppException.class);
    }
}