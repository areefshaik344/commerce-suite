package com.commercesuite.checkout.dto;

import com.commercesuite.checkout.entity.CheckoutSession;
import com.commercesuite.checkout.entity.CheckoutStatus;
import com.commercesuite.checkout.entity.PaymentMethodKind;
import com.commercesuite.checkout.entity.ShippingMethodKind;
import java.time.Instant;
import java.util.UUID;

public record CheckoutSessionDto(
        UUID id, UUID userId, UUID cartId, CheckoutStatus status, String currency,
        UUID addressId, ShippingMethodKind shippingMethod, Long shippingAmountPaise,
        PaymentMethodKind paymentMethod, String couponCode,
        PricingBreakdown pricing, Instant expiresAt, Instant updatedAt) {
    public static CheckoutSessionDto from(CheckoutSession s) {
        PricingBreakdown p = new PricingBreakdown(
                s.getSubtotalPaise(), s.getDiscountPaise(), s.getCouponDiscountPaise(),
                s.getShippingAmountPaise() == null ? 0 : s.getShippingAmountPaise(),
                s.getTaxPaise(), s.getPlatformFeePaise(), s.getGrandTotalPaise(), s.getCurrency());
        return new CheckoutSessionDto(s.getId(), s.getUserId(), s.getCartId(), s.getStatus(),
                s.getCurrency(), s.getAddressId(), s.getShippingMethod(), s.getShippingAmountPaise(),
                s.getPaymentMethod(), s.getCouponCode(), p, s.getExpiresAt(), s.getUpdatedAt());
    }
}