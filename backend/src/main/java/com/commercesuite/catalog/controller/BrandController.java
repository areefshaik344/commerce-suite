package com.commercesuite.catalog.controller;

import com.commercesuite.catalog.dto.BrandDto;
import com.commercesuite.catalog.dto.UpsertBrandRequest;
import com.commercesuite.catalog.service.BrandService;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Brands")
@RestController
@RequiredArgsConstructor
public class BrandController {
    private final BrandService service;

    @GetMapping("/api/v1/catalog/brands")
    public ApiResponse<List<BrandDto>> publicList() { return ApiResponse.ok(service.listActive()); }

    @PostMapping("/api/v1/admin/brands")
    @RequiresPermission(Permissions.MANAGE_CATEGORIES)
    public ResponseEntity<ApiResponse<BrandDto>> create(@Valid @RequestBody UpsertBrandRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r), "Brand created"));
    }

    @PutMapping("/api/v1/admin/brands/{id}")
    @RequiresPermission(Permissions.MANAGE_CATEGORIES)
    public ApiResponse<BrandDto> update(@PathVariable UUID id, @Valid @RequestBody UpsertBrandRequest r) {
        return ApiResponse.ok(service.update(id, r), "Brand updated");
    }

    @DeleteMapping("/api/v1/admin/brands/{id}")
    @RequiresPermission(Permissions.MANAGE_CATEGORIES)
    public ApiResponse<Void> delete(@PathVariable UUID id) { service.delete(id); return ApiResponse.ok(null, "Deleted"); }
}