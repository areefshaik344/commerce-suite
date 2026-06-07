package com.commercesuite.catalog.controller;

import com.commercesuite.catalog.dto.*;
import com.commercesuite.catalog.entity.ProductStatus;
import com.commercesuite.catalog.service.ProductService;
import com.commercesuite.catalog.service.ProductVariantService;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.rbac.service.Permissions;
import com.commercesuite.rbac.service.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Products")
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductVariantService variantService;
    private final ActorContextHolder actor;

    /* ----- Vendor (self) ----- */

    @PostMapping("/api/v1/products")
    @RequiresPermission(Permissions.MANAGE_PRODUCTS)
    public ResponseEntity<ApiResponse<ProductDto>> create(@Valid @RequestBody CreateProductRequest r) {
        var out = productService.create(actor.require(), r);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(out, "Product created"));
    }

    @PutMapping("/api/v1/products/{id}")
    @RequiresPermission(Permissions.MANAGE_PRODUCTS)
    public ApiResponse<ProductDto> update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest r) {
        return ApiResponse.ok(productService.update(actor.require(), id, r), "Product updated");
    }

    @GetMapping("/api/v1/products/{id}")
    @RequiresPermission(Permissions.MANAGE_PRODUCTS)
    public ApiResponse<ProductDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(productService.get(actor.require(), id));
    }

    @GetMapping("/api/v1/products/mine")
    @RequiresPermission(Permissions.MANAGE_PRODUCTS)
    public ApiResponse<PageResponse<ProductDto>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = productService.mine(actor.require(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.ok(PageResponse.of(p));
    }

    @PostMapping("/api/v1/products/{id}/submit")
    @RequiresPermission(Permissions.MANAGE_PRODUCTS)
    public ApiResponse<ProductDto> submit(@PathVariable UUID id) {
        return ApiResponse.ok(productService.submit(actor.require(), id), "Submitted for review");
    }

    @PostMapping("/api/v1/products/{id}/archive")
    @RequiresPermission(Permissions.MANAGE_PRODUCTS)
    public ApiResponse<ProductDto> archive(@PathVariable UUID id) {
        return ApiResponse.ok(productService.archive(actor.require(), id), "Archived");
    }

    /* ----- Variants (vendor) ----- */

    @PostMapping("/api/v1/products/{id}/variants")
    @RequiresPermission(Permissions.MANAGE_PRODUCTS)
    public ResponseEntity<ApiResponse<ProductVariantDto>> addVariant(@PathVariable UUID id,
                                                                     @Valid @RequestBody UpsertVariantRequest r) {
        var out = variantService.create(actor.require(), id, r);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(out, "Variant created"));
    }

    @GetMapping("/api/v1/products/{id}/variants")
    @RequiresPermission(Permissions.MANAGE_PRODUCTS)
    public ApiResponse<List<ProductVariantDto>> listVariants(@PathVariable UUID id) {
        return ApiResponse.ok(variantService.list(actor.require(), id));
    }

    /* ----- Public storefront ----- */

    @GetMapping("/api/v1/catalog/products")
    public ApiResponse<PageResponse<ProductDto>> publicSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var crit = new ProductSearchCriteria(keyword, categoryId, brandId, vendorId, ProductStatus.APPROVED);
        var p = productService.publicSearch(crit,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.ok(PageResponse.of(p));
    }

    @GetMapping("/api/v1/catalog/products/{slug}")
    public ApiResponse<ProductDto> publicBySlug(@PathVariable String slug) {
        return ApiResponse.ok(productService.publicBySlug(slug));
    }
}