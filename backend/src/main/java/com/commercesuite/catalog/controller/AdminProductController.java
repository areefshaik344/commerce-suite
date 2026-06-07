package com.commercesuite.catalog.controller;

import com.commercesuite.catalog.dto.ModerationActionRequest;
import com.commercesuite.catalog.dto.ProductDto;
import com.commercesuite.catalog.service.ProductModerationService;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Products (admin)")
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductModerationService moderation;
    private final ActorContextHolder actor;

    @PostMapping("/{id}/approve")
    @RequiresPermission(Permissions.MODERATE_PRODUCTS)
    public ApiResponse<ProductDto> approve(@PathVariable UUID id,
                                           @Valid @RequestBody(required = false) ModerationActionRequest r) {
        return ApiResponse.ok(moderation.approve(id, actor.require().userId(), reason(r)), "Product approved");
    }

    @PostMapping("/{id}/reject")
    @RequiresPermission(Permissions.MODERATE_PRODUCTS)
    public ApiResponse<ProductDto> reject(@PathVariable UUID id,
                                          @Valid @RequestBody(required = false) ModerationActionRequest r) {
        return ApiResponse.ok(moderation.reject(id, actor.require().userId(), reason(r)), "Product rejected");
    }

    @PostMapping("/{id}/suspend")
    @RequiresPermission(Permissions.MODERATE_PRODUCTS)
    public ApiResponse<ProductDto> suspend(@PathVariable UUID id,
                                           @Valid @RequestBody(required = false) ModerationActionRequest r) {
        return ApiResponse.ok(moderation.suspend(id, actor.require().userId(), reason(r)), "Product suspended");
    }

    private static String reason(ModerationActionRequest r) { return r == null ? null : r.reason(); }
}