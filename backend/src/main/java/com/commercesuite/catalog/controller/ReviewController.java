package com.commercesuite.catalog.controller;

import com.commercesuite.catalog.dto.CreateReviewRequest;
import com.commercesuite.catalog.dto.ProductReviewDto;
import com.commercesuite.catalog.service.ProductReviewService;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
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

@Tag(name = "Reviews")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ProductReviewService reviewService;
    private final ActorContextHolder actor;

    @PostMapping("/api/v1/products/{id}/reviews")
    @RequiresPermission(Permissions.WRITE_REVIEW)
    public ResponseEntity<ApiResponse<ProductReviewDto>> create(@PathVariable("id") UUID productId,
                                                                 @Valid @RequestBody CreateReviewRequest r) {
        var out = reviewService.create(productId, actor.require().userId(), r);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(out, "Review submitted"));
    }

    @GetMapping("/api/v1/catalog/products/{id}/reviews")
    public ApiResponse<PageResponse<ProductReviewDto>> list(@PathVariable("id") UUID productId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        var p = reviewService.list(productId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.ok(PageResponse.of(p));
    }

    @DeleteMapping("/api/v1/reviews/{reviewId}")
    @RequiresPermission(Permissions.WRITE_REVIEW)
    public ApiResponse<Void> delete(@PathVariable UUID reviewId) {
        reviewService.delete(reviewId, actor.require().userId());
        return ApiResponse.ok(null, "Review deleted");
    }
}