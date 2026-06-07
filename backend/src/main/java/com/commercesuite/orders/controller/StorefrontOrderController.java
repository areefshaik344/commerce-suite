package com.commercesuite.orders.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.PageResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.orders.dto.storefront.*;
import com.commercesuite.orders.service.storefront.StorefrontOrderReadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Authenticated, customer-scoped read API. Provides aggregated DTOs so the
 * storefront order screens do not need to call multiple domains.
 */
@Tag(name = "Storefront / Orders")
@RestController
@RequestMapping("/api/v1/storefront")
@RequiredArgsConstructor
public class StorefrontOrderController {

    private final StorefrontOrderReadService read;
    private final ActorContextHolder actor;

    @GetMapping("/orders")
    public ApiResponse<PageResponse<OrderCardDto>> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.of(read.listForCustomer(actor.require(), page, size)));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderDetailDto> detail(@PathVariable UUID id) {
        return ApiResponse.ok(read.detail(id, actor.require()));
    }

    @GetMapping("/orders/{id}/timeline")
    public ApiResponse<OrderTimelineDto> timeline(@PathVariable UUID id) {
        return ApiResponse.ok(read.timeline(id, actor.require()));
    }

    @GetMapping("/orders/{id}/shipments")
    public ApiResponse<List<ShipmentSummaryDto>> shipments(@PathVariable UUID id) {
        return ApiResponse.ok(read.shipments(id, actor.require()));
    }

    @GetMapping("/orders/{id}/returns")
    public ApiResponse<List<ReturnSummaryDto>> returns(@PathVariable UUID id) {
        return ApiResponse.ok(read.returns(id, actor.require()));
    }

    @GetMapping("/orders/{id}/refunds")
    public ApiResponse<List<RefundSummaryDto>> refunds(@PathVariable UUID id) {
        return ApiResponse.ok(read.refunds(id, actor.require()));
    }

    @GetMapping("/shipments/{id}")
    public ApiResponse<ShipmentSummaryDto> shipment(@PathVariable UUID id) {
        return ApiResponse.ok(read.shipment(id, actor.require()));
    }
}