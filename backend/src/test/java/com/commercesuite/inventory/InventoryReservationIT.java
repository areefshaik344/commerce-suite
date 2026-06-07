package com.commercesuite.inventory;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.catalog.service.BrandService;
import com.commercesuite.catalog.service.CategoryService;
import com.commercesuite.catalog.service.ProductService;
import com.commercesuite.catalog.service.ProductVariantService;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.inventory.dto.AdjustInventoryRequest;
import com.commercesuite.inventory.dto.ReserveInventoryRequest;
import com.commercesuite.inventory.entity.InventoryAdjustmentReason;
import com.commercesuite.inventory.entity.ReservationReleaseReason;
import com.commercesuite.inventory.entity.ReservationStatus;
import com.commercesuite.inventory.repository.InventoryItemRepository;
import com.commercesuite.inventory.repository.InventoryReservationRepository;
import com.commercesuite.inventory.service.InventoryAdjustmentService;
import com.commercesuite.inventory.service.InventoryReservationService;
import com.commercesuite.inventory.service.InventoryService;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.vendor.service.VendorAdminService;
import com.commercesuite.vendor.service.VendorApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryReservationIT extends AbstractIT {

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
    @Autowired InventoryReservationService reservationService;
    @Autowired InventoryItemRepository itemRepo;
    @Autowired InventoryReservationRepository reservationRepo;

    @Test
    void reserveCommitDecrementsOnHandAndReleasesReserved() {
        var vv = InventoryTestSupport.vendorWithVariant(auth, roles, apps, vendorAdmin,
                categories, brands, products, variants, "resv");
        var actor = InventoryTestSupport.actorOf(vv.vendor(), AppRole.VENDOR);

        inventoryService.ensureInventoryFor(vv.variantId(), actor);
        adjustmentService.adjust(vv.variantId(),
                new AdjustInventoryRequest(InventoryAdjustmentReason.INCREASE, 100, "seed"), actor);

        var res = reservationService.reserve(vv.variantId(),
                new ReserveInventoryRequest(3, 19900L, null, null), actor);

        var item = itemRepo.findByVariantId(vv.variantId()).orElseThrow();
        assertThat(item.getOnHandQty()).isEqualTo(100);
        assertThat(item.getReservedQty()).isEqualTo(3);
        assertThat(item.getAvailableQty()).isEqualTo(97);

        reservationService.commit(res.id(), actor);

        var after = itemRepo.findByVariantId(vv.variantId()).orElseThrow();
        assertThat(after.getOnHandQty()).isEqualTo(97);
        assertThat(after.getReservedQty()).isZero();
        assertThat(reservationRepo.findById(res.id()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.COMMITTED);
    }

    @Test
    void releaseRestoresReservedQuantity() {
        var vv = InventoryTestSupport.vendorWithVariant(auth, roles, apps, vendorAdmin,
                categories, brands, products, variants, "rel");
        var actor = InventoryTestSupport.actorOf(vv.vendor(), AppRole.VENDOR);
        inventoryService.ensureInventoryFor(vv.variantId(), actor);
        adjustmentService.adjust(vv.variantId(),
                new AdjustInventoryRequest(InventoryAdjustmentReason.INCREASE, 10, "seed"), actor);

        var res = reservationService.reserve(vv.variantId(),
                new ReserveInventoryRequest(4, 1000L, null, null), actor);
        reservationService.release(res.id(), ReservationReleaseReason.ABANDONED, actor);

        var item = itemRepo.findByVariantId(vv.variantId()).orElseThrow();
        assertThat(item.getReservedQty()).isZero();
        assertThat(item.getOnHandQty()).isEqualTo(10);
        assertThat(reservationRepo.findById(res.id()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void reservingMoreThanAvailableFails() {
        var vv = InventoryTestSupport.vendorWithVariant(auth, roles, apps, vendorAdmin,
                categories, brands, products, variants, "over");
        var actor = InventoryTestSupport.actorOf(vv.vendor(), AppRole.VENDOR);
        inventoryService.ensureInventoryFor(vv.variantId(), actor);
        adjustmentService.adjust(vv.variantId(),
                new AdjustInventoryRequest(InventoryAdjustmentReason.INCREASE, 2, "seed"), actor);

        assertThatThrownBy(() -> reservationService.reserve(vv.variantId(),
                new ReserveInventoryRequest(5, 1000L, null, null), actor))
                .isInstanceOf(AppException.class);
    }

    @Test
    void doubleCommitFails() {
        var vv = InventoryTestSupport.vendorWithVariant(auth, roles, apps, vendorAdmin,
                categories, brands, products, variants, "dbl");
        var actor = InventoryTestSupport.actorOf(vv.vendor(), AppRole.VENDOR);
        inventoryService.ensureInventoryFor(vv.variantId(), actor);
        adjustmentService.adjust(vv.variantId(),
                new AdjustInventoryRequest(InventoryAdjustmentReason.INCREASE, 5, "seed"), actor);

        var res = reservationService.reserve(vv.variantId(),
                new ReserveInventoryRequest(2, 1000L, null, null), actor);
        reservationService.commit(res.id(), actor);
        assertThatThrownBy(() -> reservationService.commit(res.id(), actor))
                .isInstanceOf(AppException.class);
    }
}