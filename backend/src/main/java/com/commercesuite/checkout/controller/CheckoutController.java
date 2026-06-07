package com.commercesuite.checkout.controller;

import com.commercesuite.checkout.dto.*;
import com.commercesuite.checkout.service.CheckoutService;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Checkout")
@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final ActorContextHolder actor;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<CheckoutSessionDto>> start(
            @RequestBody(required = false) StartCheckoutRequest req,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        var out = checkoutService.start(req, idempotencyKey, actor.require());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(out, "Checkout started"));
    }

    @PostMapping("/{id}/address")
    public ApiResponse<CheckoutSessionDto> address(@PathVariable UUID id,
                                                   @Valid @RequestBody SelectAddressRequest req) {
        return ApiResponse.ok(checkoutService.selectAddress(id, req, actor.require()), "Address selected");
    }

    @PostMapping("/{id}/shipping")
    public ApiResponse<CheckoutSessionDto> shipping(@PathVariable UUID id,
                                                    @Valid @RequestBody SelectShippingRequest req) {
        return ApiResponse.ok(checkoutService.selectShipping(id, req, actor.require()), "Shipping selected");
    }

    @PostMapping("/{id}/payment")
    public ApiResponse<CheckoutSessionDto> payment(@PathVariable UUID id,
                                                   @Valid @RequestBody SelectPaymentRequest req) {
        return ApiResponse.ok(checkoutService.selectPayment(id, req, actor.require()), "Payment selected");
    }

    @GetMapping("/{id}")
    public ApiResponse<CheckoutSessionDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(checkoutService.get(id, actor.require()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<CheckoutSessionDto> cancel(@PathVariable UUID id,
                                                  @RequestBody(required = false) CancelCheckoutRequest req,
                                                  @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.ok(checkoutService.cancel(id, req, idempotencyKey, actor.require()), "Cancelled");
    }
}