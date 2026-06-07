package com.commercesuite.inventory.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.inventory.dto.*;
import com.commercesuite.inventory.service.*;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Inventory")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryAdjustmentService adjustmentService;
    private final InventoryReservationService reservationService;
    private final InventoryLowStockService lowStockService;
    private final ActorContextHolder actor;

    /* ---- Items ---- */

    @GetMapping
    @RequiresPermission(Permissions.MANAGE_INVENTORY)
    public ApiResponse<PageResponse<InventoryItemDto>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = inventoryService.listMine(actor.require(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return ApiResponse.ok(PageResponse.of(p));
    }

    @GetMapping("/{variantId}")
    @RequiresPermission(Permissions.MANAGE_INVENTORY)
    public ApiResponse<InventoryItemDto> get(@PathVariable UUID variantId) {
        return ApiResponse.ok(inventoryService.getByVariant(variantId, actor.require()));
    }

    @PutMapping("/{variantId}")
    @RequiresPermission(Permissions.MANAGE_INVENTORY)
    public ApiResponse<InventoryItemDto> update(@PathVariable UUID variantId,
                                                @Valid @RequestBody UpdateInventoryRequest r) {
        return ApiResponse.ok(inventoryService.update(variantId, r, actor.require()), "Inventory updated");
    }

    @PostMapping("/{variantId}/init")
    @RequiresPermission(Permissions.MANAGE_INVENTORY)
    public ResponseEntity<ApiResponse<InventoryItemDto>> init(@PathVariable UUID variantId) {
        var out = inventoryService.ensureInventoryFor(variantId, actor.require());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(out, "Inventory initialised"));
    }

    /* ---- Adjustments ---- */

    @PostMapping("/{variantId}/adjust")
    @RequiresPermission(Permissions.MANAGE_INVENTORY)
    public ApiResponse<AdjustmentDto> adjust(@PathVariable UUID variantId,
                                             @Valid @RequestBody AdjustInventoryRequest r) {
        return ApiResponse.ok(adjustmentService.adjust(variantId, r, actor.require()), "Adjusted");
    }

    /* ---- Reservations ---- */

    @PostMapping("/{variantId}/reserve")
    @RequiresPermission(Permissions.MANAGE_INVENTORY)
    public ResponseEntity<ApiResponse<ReservationDto>> reserve(@PathVariable UUID variantId,
                                                               @Valid @RequestBody ReserveInventoryRequest r) {
        var out = reservationService.reserve(variantId, r, actor.require());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(out, "Reserved"));
    }

    @PostMapping("/reservations/{id}/commit")
    @RequiresPermission(Permissions.MANAGE_INVENTORY)
    public ApiResponse<ReservationDto> commit(@PathVariable UUID id) {
        return ApiResponse.ok(reservationService.commit(id, actor.require()), "Committed");
    }

    @PostMapping("/reservations/{id}/release")
    @RequiresPermission(Permissions.MANAGE_INVENTORY)
    public ApiResponse<ReservationDto> release(@PathVariable UUID id,
                                               @Valid @RequestBody ReleaseReservationRequest r) {
        return ApiResponse.ok(reservationService.release(id, r.reason(), actor.require()), "Released");
    }

    @GetMapping("/reservations/{id}")
    @RequiresPermission(Permissions.MANAGE_INVENTORY)
    public ApiResponse<ReservationDto> getReservation(@PathVariable UUID id) {
        return ApiResponse.ok(reservationService.get(id, actor.require()));
    }

    /* ---- Low stock ---- */

    @PutMapping("/{variantId}/low-stock-rule")
    @RequiresPermission(Permissions.MANAGE_INVENTORY)
    public ApiResponse<LowStockRuleDto> upsertRule(@PathVariable UUID variantId,
                                                   @Valid @RequestBody UpsertLowStockRuleRequest r) {
        return ApiResponse.ok(lowStockService.upsert(variantId, r, actor.require()), "Saved");
    }

    @GetMapping("/{variantId}/low-stock-rule")
    @RequiresPermission(Permissions.MANAGE_INVENTORY)
    public ApiResponse<LowStockRuleDto> getRule(@PathVariable UUID variantId) {
        return ApiResponse.ok(lowStockService.get(variantId, actor.require()));
    }
}