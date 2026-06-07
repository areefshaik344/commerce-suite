package com.commercesuite.coupon.event;

import java.time.Instant;
import java.util.UUID;

public final class CouponEvents {
    private CouponEvents() {}
    public record CouponAppliedEvent(UUID couponId, String code, UUID userId, UUID checkoutId, long discountPaise, Instant at) {}
    public record CouponRejectedEvent(String code, UUID userId, String reason, Instant at) {}
    public record CouponCommittedEvent(UUID couponId, UUID userId, UUID orderId, long discountPaise, Instant at) {}
}