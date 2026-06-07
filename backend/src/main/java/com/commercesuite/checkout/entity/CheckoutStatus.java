package com.commercesuite.checkout.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Checkout session FSM. See docs/CART_CHECKOUT_MODULE.md. */
public enum CheckoutStatus {
    CREATED,
    ADDRESS_SELECTED,
    SHIPPING_SELECTED,
    PAYMENT_SELECTED,
    READY_FOR_ORDER,
    EXPIRED,
    CANCELLED,
    CONVERTED;

    private static final Map<CheckoutStatus, Set<CheckoutStatus>> ALLOWED = Map.of(
        CREATED,           EnumSet.of(ADDRESS_SELECTED, CANCELLED, EXPIRED),
        ADDRESS_SELECTED,  EnumSet.of(ADDRESS_SELECTED, SHIPPING_SELECTED, CANCELLED, EXPIRED),
        SHIPPING_SELECTED, EnumSet.of(ADDRESS_SELECTED, SHIPPING_SELECTED, PAYMENT_SELECTED, CANCELLED, EXPIRED),
        PAYMENT_SELECTED,  EnumSet.of(ADDRESS_SELECTED, SHIPPING_SELECTED, PAYMENT_SELECTED, READY_FOR_ORDER, CANCELLED, EXPIRED),
        READY_FOR_ORDER,   EnumSet.of(CONVERTED, CANCELLED, EXPIRED),
        EXPIRED,           EnumSet.noneOf(CheckoutStatus.class),
        CANCELLED,         EnumSet.noneOf(CheckoutStatus.class),
        CONVERTED,         EnumSet.noneOf(CheckoutStatus.class)
    );

    public boolean canTransitionTo(CheckoutStatus next) {
        return ALLOWED.getOrDefault(this, EnumSet.noneOf(CheckoutStatus.class)).contains(next);
    }

    public boolean isTerminal() { return this == EXPIRED || this == CANCELLED || this == CONVERTED; }
    public boolean isActive()   { return !isTerminal(); }
}