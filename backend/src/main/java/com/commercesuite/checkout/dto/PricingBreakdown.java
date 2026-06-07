package com.commercesuite.checkout.dto;

/** Money in integer paise. MONEY_SPEC.md. */
public record PricingBreakdown(
        long subtotalPaise,
        long discountPaise,
        long couponDiscountPaise,
        long shippingPaise,
        long taxPaise,
        long platformFeePaise,
        long grandTotalPaise,
        String currency) {
    public static PricingBreakdown zero() {
        return new PricingBreakdown(0, 0, 0, 0, 0, 0, 0, "INR");
    }
}