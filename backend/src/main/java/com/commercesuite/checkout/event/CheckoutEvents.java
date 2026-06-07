package com.commercesuite.checkout.event;

import com.commercesuite.checkout.entity.CheckoutStatus;
import java.time.Instant;
import java.util.UUID;

public final class CheckoutEvents {
    private CheckoutEvents() {}
    public record CheckoutStartedEvent(UUID checkoutId, UUID userId, UUID cartId, Instant expiresAt, Instant at) {}
    public record CheckoutAddressSelectedEvent(UUID checkoutId, UUID userId, UUID addressId, Instant at) {}
    public record CheckoutShippingSelectedEvent(UUID checkoutId, UUID userId, Instant at) {}
    public record CheckoutPaymentSelectedEvent(UUID checkoutId, UUID userId, Instant at) {}
    public record CheckoutReadyForOrderEvent(UUID checkoutId, UUID userId, long grandTotalPaise, Instant at) {}
    public record CheckoutCancelledEvent(UUID checkoutId, UUID userId, String reason, Instant at) {}
    public record CheckoutExpiredEvent(UUID checkoutId, UUID userId, Instant at) {}
    public record CheckoutStateChangedEvent(UUID checkoutId, CheckoutStatus from, CheckoutStatus to, Instant at) {}
}