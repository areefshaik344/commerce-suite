package com.commercesuite.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ValidateCouponRequest(
        @NotBlank String code,
        UUID checkoutId) {}