package com.commercesuite.coupon.dto;

public record CouponValidationResult(
        boolean valid, String code, long discountPaise, long subtotalPaise,
        long grandTotalPaise, String message) {}