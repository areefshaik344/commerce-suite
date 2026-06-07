package com.commercesuite.checkout;

import static org.junit.jupiter.api.Assertions.*;

import com.commercesuite.cart.entity.CartItem;
import com.commercesuite.checkout.dto.PricingBreakdown;
import com.commercesuite.checkout.service.PricingEngine;
import com.commercesuite.coupon.entity.Coupon;
import com.commercesuite.coupon.entity.CouponType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PricingEngineTest {

    private final PricingEngine engine = new PricingEngine();

    @BeforeEach void setBps() {
        ReflectionTestUtils.setField(engine, "defaultTaxBps", 1800); // 18%
        ReflectionTestUtils.setField(engine, "platformFeeBps", 0);
    }

    private CartItem line(long unitPaise, int qty) {
        return CartItem.builder().unitPricePaise(unitPaise).qty(qty).currency("INR").build();
    }

    @Test
    void subtotalIsExactInteger() {
        var p = engine.calculate(List.of(line(12399, 2), line(50001, 1)), null, 0L, 0);
        assertEquals(12399L * 2 + 50001L, p.subtotalPaise());
        assertEquals(0, p.taxPaise());
        assertEquals(p.subtotalPaise(), p.grandTotalPaise());
    }

    @Test
    void percentageCouponClampedByCap() {
        Coupon c = Coupon.builder().type(CouponType.PERCENTAGE)
                .percentOff(new BigDecimal("20.00"))
                .maxDiscountPaise(5000L).currency("INR").build();
        var p = engine.calculate(List.of(line(100_000L, 1)), c, 0L, 0);
        assertEquals(5000L, p.couponDiscountPaise());
        assertEquals(95_000L, p.grandTotalPaise());
    }

    @Test
    void fixedAmountCouponNeverExceedsSubtotal() {
        Coupon c = Coupon.builder().type(CouponType.FIXED_AMOUNT)
                .amountOffPaise(500_000L).currency("INR").build();
        var p = engine.calculate(List.of(line(10_000L, 1)), c, 0L, 0);
        assertEquals(10_000L, p.couponDiscountPaise());
        assertEquals(0L, p.grandTotalPaise());
    }

    @Test
    void freeShippingCouponZeroesShipping() {
        Coupon c = Coupon.builder().type(CouponType.FREE_SHIPPING).currency("INR").build();
        var p = engine.calculate(List.of(line(20_000L, 1)), c, 5_000L, 0);
        assertEquals(0L, p.shippingPaise());
        assertEquals(0L, p.couponDiscountPaise());
        assertEquals(20_000L, p.grandTotalPaise());
    }

    @Test
    void taxAppliedToDiscountedBase() {
        var p = engine.calculate(List.of(line(100_000L, 1)), null, 0L, 1800);
        // 18% of 100000 = 18000
        assertEquals(18_000L, p.taxPaise());
        assertEquals(118_000L, p.grandTotalPaise());
    }

    @Test
    void deterministicSameInputsSameOutput() {
        PricingBreakdown a = engine.calculate(List.of(line(123L, 7)), null, 99L, 1800);
        PricingBreakdown b = engine.calculate(List.of(line(123L, 7)), null, 99L, 1800);
        assertEquals(a, b);
    }
}