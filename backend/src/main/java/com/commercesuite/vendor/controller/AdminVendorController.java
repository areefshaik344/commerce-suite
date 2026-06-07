package com.commercesuite.vendor.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import com.commercesuite.vendor.dto.AdminVendorActionRequest;
import com.commercesuite.vendor.dto.VendorDto;
import com.commercesuite.vendor.entity.VendorStatus;
import com.commercesuite.vendor.service.VendorAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Vendor (admin)")
@RestController
@RequestMapping("/api/v1/admin/vendors")
@RequiredArgsConstructor
public class AdminVendorController {

    private final VendorAdminService admin;
    private final ActorContextHolder actor;

    @GetMapping
    @RequiresPermission({Permissions.MANAGE_VENDORS, Permissions.APPROVE_VENDOR_APPLICATIONS})
    public ApiResponse<PageResponse<VendorDto>> list(
            @RequestParam(required = false) VendorStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = admin.list(status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.ok(new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages()));
    }

    @GetMapping("/{id}")
    @RequiresPermission({Permissions.MANAGE_VENDORS, Permissions.APPROVE_VENDOR_APPLICATIONS})
    public ApiResponse<VendorDto> get(@PathVariable UUID id) { return ApiResponse.ok(admin.get(id)); }

    @PostMapping("/{id}/approve")
    @RequiresPermission(Permissions.APPROVE_VENDOR_APPLICATIONS)
    public ApiResponse<VendorDto> approve(@PathVariable UUID id, @Valid @RequestBody(required = false) AdminVendorActionRequest r) {
        return ApiResponse.ok(admin.approve(id, actor.require().userId(), reason(r)), "Vendor approved");
    }

    @PostMapping("/{id}/reject")
    @RequiresPermission(Permissions.APPROVE_VENDOR_APPLICATIONS)
    public ApiResponse<VendorDto> reject(@PathVariable UUID id, @Valid @RequestBody(required = false) AdminVendorActionRequest r) {
        return ApiResponse.ok(admin.reject(id, actor.require().userId(), reason(r)), "Vendor rejected");
    }

    @PostMapping("/{id}/suspend")
    @RequiresPermission(Permissions.MANAGE_VENDORS)
    public ApiResponse<VendorDto> suspend(@PathVariable UUID id, @Valid @RequestBody(required = false) AdminVendorActionRequest r) {
        return ApiResponse.ok(admin.suspend(id, actor.require().userId(), reason(r)), "Vendor suspended");
    }

    @PostMapping("/{id}/reactivate")
    @RequiresPermission(Permissions.MANAGE_VENDORS)
    public ApiResponse<VendorDto> reactivate(@PathVariable UUID id, @Valid @RequestBody(required = false) AdminVendorActionRequest r) {
        return ApiResponse.ok(admin.reactivate(id, actor.require().userId(), reason(r)), "Vendor reactivated");
    }

    @PostMapping("/{id}/deactivate")
    @RequiresPermission(Permissions.MANAGE_VENDORS)
    public ApiResponse<VendorDto> deactivate(@PathVariable UUID id, @Valid @RequestBody(required = false) AdminVendorActionRequest r) {
        return ApiResponse.ok(admin.deactivate(id, actor.require().userId(), reason(r)), "Vendor deactivated");
    }

    private static String reason(AdminVendorActionRequest r) { return r == null ? null : r.reason(); }
}