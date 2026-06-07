package com.commercesuite.coupon.service;

import com.commercesuite.cart.entity.CartItem;
import com.commercesuite.checkout.service.PricingEngine;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.coupon.dto.CouponValidationResult;
import com.commercesuite.coupon.entity.Coupon;
import com.commercesuite.coupon.entity.CouponScope;
import com.commercesuite.coupon.entity.CouponUsage;
import com.commercesuite.coupon.event.CouponEvents.*;
import com.commercesuite.coupon.repository.CouponRepository;
import com.commercesuite.coupon.repository.CouponUsageRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepo;
    private final CouponUsageRepository usageRepo;
    private final PricingEngine pricing;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /** Load and check active window + limits. Throws AppException with friendly message. */
    /** BLOCKER B-04: must run inside a write tx and pessimistically lock the
     *  coupon row before reading aggregated usage counts. */
    @Transactional
    public Coupon resolve(String code, UUID userId, long subtotalPaise, List<CartItem> items) {
        Coupon c = couponRepo.findByCodeForUpdate(code)
                .orElseThrow(() -> AppException.notFound("Coupon"));
        Instant now = Instant.now(clock);
        if (!c.isActive())                       reject(userId, code, "Coupon inactive");
        if (now.isBefore(c.getStartsAt()))       reject(userId, code, "Coupon not yet active");
        if (now.isAfter(c.getEndsAt()))          reject(userId, code, "Coupon expired");
        if (subtotalPaise < c.getMinOrderPaise())
            reject(userId, code, "Order subtotal below minimum");

        if (c.getUsageLimitTotal() != null
                && usageRepo.countByCouponId(c.getId()) >= c.getUsageLimitTotal())
            reject(userId, code, "Coupon usage limit reached");
        if (c.getUsageLimitPerUser() != null
                && usageRepo.countByCouponIdAndUserId(c.getId(), userId) >= c.getUsageLimitPerUser())
            reject(userId, code, "Per-user limit reached for this coupon");

        // Scope checks against cart contents.
        if (c.getScope() == CouponScope.VENDOR && c.getVendorId() != null) {
            boolean ok = items.stream().anyMatch(i -> c.getVendorId().equals(i.getVendorId()));
            if (!ok) reject(userId, code, "Coupon not applicable to selected vendor");
        }
        // CATEGORY scope deferred: requires product->category lookup.
        return c;
    }

    @Transactional(readOnly = true)
    public CouponValidationResult preview(String code, UUID userId, List<CartItem> items, long shippingPaise) {
        long subtotal = items.stream().mapToLong(i ->
                Math.multiplyExact(i.getUnitPricePaise(), (long) i.getQty())).sum();
        Coupon c = resolve(code, userId, subtotal, items);
        long discount = pricing.computeCouponDiscount(c, subtotal);
        long total = Math.max(0, subtotal - discount) + shippingPaise;
        return new CouponValidationResult(true, c.getCode(), discount, subtotal, total,
                "Coupon applied: " + c.getCode());
    }

    @Transactional
    public CouponUsage recordApplication(Coupon coupon, UUID userId, UUID checkoutId, long discountPaise) {
        CouponUsage u = usageRepo.save(CouponUsage.builder()
                .couponId(coupon.getId()).userId(userId).checkoutId(checkoutId)
                .discountPaise(discountPaise).committed(false)
                .appliedAt(Instant.now(clock)).build());
        events.publishEvent(new CouponAppliedEvent(coupon.getId(), coupon.getCode(),
                userId, checkoutId, discountPaise, Instant.now(clock)));
        return u;
    }

    private void reject(UUID userId, String code, String reason) {
        events.publishEvent(new CouponRejectedEvent(code, userId, reason, Instant.now(clock)));
        throw AppException.conflict(ErrorCode.CONFLICT, reason);
    }
}