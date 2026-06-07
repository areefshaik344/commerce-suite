package com.commercesuite.inventory.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.inventory.dto.AdjustInventoryRequest;
import com.commercesuite.inventory.dto.AdjustmentDto;
import com.commercesuite.inventory.dto.InventoryItemDto;
import com.commercesuite.inventory.service.InventoryAdjustmentService;
import com.commercesuite.inventory.service.InventoryService;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Inventory")
@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final InventoryAdjustmentService adjustmentService;
    private final ActorContextHolder actor;

    @GetMapping
    @RequiresPermission(Permissions.MODERATE_PRODUCTS)
    public ApiResponse<PageResponse<InventoryItemDto>> list(
            @RequestParam UUID vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = inventoryService.listForVendor(vendorId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return ApiResponse.ok(PageResponse.of(p));
    }

    @PostMapping("/{variantId}/adjust")
    @RequiresPermission(Permissions.MODERATE_PRODUCTS)
    public ApiResponse<AdjustmentDto> adjust(@PathVariable UUID variantId,
                                             @Valid @RequestBody AdjustInventoryRequest r) {
        return ApiResponse.ok(adjustmentService.adjust(variantId, r, actor.require()), "Adjusted");
    }
}