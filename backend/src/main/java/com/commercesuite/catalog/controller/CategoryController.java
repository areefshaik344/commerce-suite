package com.commercesuite.catalog.controller;

import com.commercesuite.catalog.dto.CategoryDto;
import com.commercesuite.catalog.dto.UpsertCategoryRequest;
import com.commercesuite.catalog.service.CategoryService;
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

@Tag(name = "Categories")
@RestController
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    /** Public tree (used by storefront). */
    @GetMapping("/api/v1/catalog/categories")
    public ApiResponse<List<CategoryDto>> publicTree() { return ApiResponse.ok(service.tree()); }

    @PostMapping("/api/v1/admin/categories")
    @RequiresPermission(Permissions.MANAGE_CATEGORIES)
    public ResponseEntity<ApiResponse<CategoryDto>> create(@Valid @RequestBody UpsertCategoryRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(r), "Category created"));
    }

    @PutMapping("/api/v1/admin/categories/{id}")
    @RequiresPermission(Permissions.MANAGE_CATEGORIES)
    public ApiResponse<CategoryDto> update(@PathVariable UUID id, @Valid @RequestBody UpsertCategoryRequest r) {
        return ApiResponse.ok(service.update(id, r), "Category updated");
    }

    @DeleteMapping("/api/v1/admin/categories/{id}")
    @RequiresPermission(Permissions.MANAGE_CATEGORIES)
    public ApiResponse<Void> delete(@PathVariable UUID id) { service.delete(id); return ApiResponse.ok(null, "Deleted"); }
}