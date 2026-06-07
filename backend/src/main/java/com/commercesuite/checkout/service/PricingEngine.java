package com.commercesuite.checkout.service;

import com.commercesuite.cart.entity.CartItem;
import com.commercesuite.checkout.dto.PricingBreakdown;
import com.commercesuite.coupon.entity.Coupon;
import com.commercesuite.coupon.entity.CouponType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Deterministic, integer-paise pricing. MONEY_SPEC.md compliant.
 * No double/float anywhere; percentage math uses BigDecimal then rounded once.
 */
@Service
public class PricingEngine {

    @Value("${app.pricing.tax-bps:0}")        private int defaultTaxBps;
    @Value("${app.pricing.platform-fee-bps:0}") private int platformFeeBps;

    public PricingBreakdown calculate(List<CartItem> items, Coupon coupon,
                                      Long shippingPaise, Integer overrideTaxBps) {
        long subtotal = 0;
        for (CartItem i : items) {
            subtotal = Math.addExact(subtotal, Math.multiplyExact(i.getUnitPricePaise(), (long) i.getQty()));
        }

        long couponDiscount = computeCouponDiscount(coupon, subtotal);
        long discount = couponDiscount;          // (room for line-level promos later)
        long shipping = computeShipping(coupon, shippingPaise);

        long taxableBase = Math.max(0L, subtotal - discount);
        int taxBps = overrideTaxBps != null ? overrideTaxBps : defaultTaxBps;
        long tax = applyBps(taxableBase, taxBps);
        long platformFee = applyBps(taxableBase, platformFeeBps);
        long total = Math.max(0L,
                Math.addExact(Math.addExact(Math.addExact(taxableBase, shipping), tax), platformFee));

        return new PricingBreakdown(subtotal, discount, couponDiscount, shipping, tax,
                platformFee, total, "INR");
    }

    private long computeShipping(Coupon coupon, Long shippingPaise) {
        long s = shippingPaise == null ? 0L : shippingPaise;
        if (coupon != null && coupon.getType() == CouponType.FREE_SHIPPING) return 0L;
        return s;
    }

    long computeCouponDiscount(Coupon coupon, long subtotal) {
        if (coupon == null) return 0L;
        return switch (coupon.getType()) {
            case FREE_SHIPPING -> 0L;
            case FIXED_AMOUNT -> Math.min(subtotal,
                    coupon.getAmountOffPaise() == null ? 0L : coupon.getAmountOffPaise());
            case PERCENTAGE -> {
                if (coupon.getPercentOff() == null) yield 0L;
                BigDecimal pct = coupon.getPercentOff()
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                long raw = pct.multiply(BigDecimal.valueOf(subtotal))
                        .setScale(0, RoundingMode.HALF_UP).longValueExact();
                if (coupon.getMaxDiscountPaise() != null) raw = Math.min(raw, coupon.getMaxDiscountPaise());
                yield Math.min(raw, subtotal);
            }
        };
    }

    private long applyBps(long base, int bps) {
        if (bps <= 0) return 0L;
        return BigDecimal.valueOf(base)
                .multiply(BigDecimal.valueOf(bps))
                .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}