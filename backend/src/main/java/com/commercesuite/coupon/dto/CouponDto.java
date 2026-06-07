package com.commercesuite.coupon.dto;

import com.commercesuite.coupon.entity.Coupon;
import com.commercesuite.coupon.entity.CouponScope;
import com.commercesuite.coupon.entity.CouponType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponDto(
        UUID id, String code, String label, CouponType type, CouponScope scope,
        BigDecimal percentOff, Long amountOffPaise, Long maxDiscountPaise,
        long minOrderPaise, UUID vendorId, UUID categoryId,
        Instant startsAt, Instant endsAt, Integer usageLimitTotal,
        Integer usageLimitPerUser, boolean active) {
    public static CouponDto from(Coupon c) {
        return new CouponDto(c.getId(), c.getCode(), c.getLabel(), c.getType(), c.getScope(),
                c.getPercentOff(), c.getAmountOffPaise(), c.getMaxDiscountPaise(),
                c.getMinOrderPaise(), c.getVendorId(), c.getCategoryId(),
                c.getStartsAt(), c.getEndsAt(), c.getUsageLimitTotal(),
                c.getUsageLimitPerUser(), c.isActive());
    }
}