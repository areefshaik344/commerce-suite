package com.commercesuite.coupon.controller;

import com.commercesuite.cart.entity.Cart;
import com.commercesuite.cart.entity.CartItem;
import com.commercesuite.cart.entity.CartStatus;
import com.commercesuite.cart.repository.CartItemRepository;
import com.commercesuite.cart.repository.CartRepository;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.coupon.dto.CouponValidationResult;
import com.commercesuite.coupon.dto.ValidateCouponRequest;
import com.commercesuite.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Coupons")
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ActorContextHolder actor;

    @PostMapping("/validate")
    public ApiResponse<CouponValidationResult> validate(@Valid @RequestBody ValidateCouponRequest req) {
        var a = actor.require();
        Cart cart = cartRepo.findByUserIdAndStatus(a.userId(), CartStatus.ACTIVE)
                .orElseThrow(() -> AppException.conflict(ErrorCode.CONFLICT, "No active cart"));
        List<CartItem> items = cartItemRepo.findByCartId(cart.getId());
        var result = couponService.preview(req.code(), a.userId(), items, 0L);
        return ApiResponse.ok(result, result.message());
    }
}