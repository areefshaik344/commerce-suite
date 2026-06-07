package com.commercesuite.catalog.controller;

import com.commercesuite.catalog.dto.storefront.*;
import com.commercesuite.catalog.service.storefront.StorefrontReadService;
import com.commercesuite.catalog.service.storefront.StorefrontSearchCriteria;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Public, read-only storefront API. Returns aggregated DTOs designed for
 * the storefront UI so the frontend does no cross-domain joins.
 */
@Tag(name = "Storefront")
@RestController
@RequestMapping("/api/v1/storefront")
@RequiredArgsConstructor
public class StorefrontController {

    private final StorefrontReadService read;

    @GetMapping("/products")
    public ApiResponse<ProductSearchResultDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) List<UUID> brandIds,
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(required = false) Long minPricePaise,
            @RequestParam(required = false) Long maxPricePaise,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        StorefrontSearchCriteria c = new StorefrontSearchCriteria(
                q, categoryId, categorySlug, brandIds, vendorId,
                minPricePaise, maxPricePaise, minRating);
        return ApiResponse.ok(read.search(c, page, size, sort));
    }

    @GetMapping("/products/{slug}")
    public ApiResponse<ProductDetailDto> detail(@PathVariable String slug) {
        return ApiResponse.ok(read.detailBySlug(slug));
    }

    @GetMapping("/brands")
    public ApiResponse<List<BrandFilterDto>> brands() {
        return ApiResponse.ok(read.brandFilter());
    }

    @GetMapping("/products/{id}/reviews/summary")
    public ApiResponse<ReviewSummaryDto> reviewSummary(@PathVariable("id") UUID productId) {
        return ApiResponse.ok(read.reviewSummary(productId));
    }

    @GetMapping("/products/{id}/reviews")
    public ApiResponse<PageResponse<ReviewItemDto>> reviews(
            @PathVariable("id") UUID productId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(read.listReviews(productId, page, size));
    }

    @GetMapping("/suggest")
    public ApiResponse<List<String>> suggest(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok(read.suggest(q, limit));
    }
}