package com.commercesuite.inventory;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.catalog.service.BrandService;
import com.commercesuite.catalog.service.CategoryService;
import com.commercesuite.catalog.service.ProductService;
import com.commercesuite.catalog.service.ProductVariantService;
import com.commercesuite.inventory.dto.AdjustInventoryRequest;
import com.commercesuite.inventory.dto.UpsertLowStockRuleRequest;
import com.commercesuite.inventory.entity.InventoryAdjustmentReason;
import com.commercesuite.inventory.service.InventoryAdjustmentService;
import com.commercesuite.inventory.service.InventoryLowStockService;
import com.commercesuite.inventory.service.InventoryService;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.vendor.service.VendorAdminService;
import com.commercesuite.vendor.service.VendorApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class LowStockIT extends AbstractIT {

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
    @Autowired InventoryLowStockService lowStockService;

    @Test
    void upsertAndReadRule() {
        var vv = InventoryTestSupport.vendorWithVariant(auth, roles, apps, vendorAdmin,
                categories, brands, products, variants, "ls");
        var actor = InventoryTestSupport.actorOf(vv.vendor(), AppRole.VENDOR);
        inventoryService.ensureInventoryFor(vv.variantId(), actor);
        adjustmentService.adjust(vv.variantId(),
                new AdjustInventoryRequest(InventoryAdjustmentReason.INCREASE, 10, "seed"), actor);

        var rule = lowStockService.upsert(vv.variantId(),
                new UpsertLowStockRuleRequest(5, true), actor);

        assertThat(rule.threshold()).isEqualTo(5);
        assertThat(rule.enabled()).isTrue();
        assertThat(lowStockService.get(vv.variantId(), actor).threshold()).isEqualTo(5);
    }
}