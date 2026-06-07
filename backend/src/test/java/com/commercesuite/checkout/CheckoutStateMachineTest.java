package com.commercesuite.checkout;

import static org.junit.jupiter.api.Assertions.*;

import com.commercesuite.checkout.entity.CheckoutStatus;
import org.junit.jupiter.api.Test;

class CheckoutStateMachineTest {

    @Test
    void linearHappyPath() {
        assertTrue(CheckoutStatus.CREATED.canTransitionTo(CheckoutStatus.ADDRESS_SELECTED));
        assertTrue(CheckoutStatus.ADDRESS_SELECTED.canTransitionTo(CheckoutStatus.SHIPPING_SELECTED));
        assertTrue(CheckoutStatus.SHIPPING_SELECTED.canTransitionTo(CheckoutStatus.PAYMENT_SELECTED));
        assertTrue(CheckoutStatus.PAYMENT_SELECTED.canTransitionTo(CheckoutStatus.READY_FOR_ORDER));
        assertTrue(CheckoutStatus.READY_FOR_ORDER.canTransitionTo(CheckoutStatus.CONVERTED));
    }

    @Test
    void canBackstepWithinActiveStates() {
        assertTrue(CheckoutStatus.SHIPPING_SELECTED.canTransitionTo(CheckoutStatus.ADDRESS_SELECTED));
        assertTrue(CheckoutStatus.PAYMENT_SELECTED.canTransitionTo(CheckoutStatus.SHIPPING_SELECTED));
    }

    @Test
    void cancelOrExpireFromAnyActiveState() {
        for (CheckoutStatus s : new CheckoutStatus[]{
                CheckoutStatus.CREATED, CheckoutStatus.ADDRESS_SELECTED,
                CheckoutStatus.SHIPPING_SELECTED, CheckoutStatus.PAYMENT_SELECTED,
                CheckoutStatus.READY_FOR_ORDER}) {
            assertTrue(s.canTransitionTo(CheckoutStatus.CANCELLED), s.name());
            assertTrue(s.canTransitionTo(CheckoutStatus.EXPIRED), s.name());
        }
    }

    @Test
    void terminalStatesAreFrozen() {
        for (CheckoutStatus t : new CheckoutStatus[]{
                CheckoutStatus.EXPIRED, CheckoutStatus.CANCELLED, CheckoutStatus.CONVERTED}) {
            assertTrue(t.isTerminal());
            for (CheckoutStatus n : CheckoutStatus.values()) {
                assertFalse(t.canTransitionTo(n), t + "->" + n);
            }
        }
    }

    @Test
    void cannotSkipBackToCreated() {
        for (CheckoutStatus s : CheckoutStatus.values()) {
            assertFalse(s.canTransitionTo(CheckoutStatus.CREATED));
        }
    }

    @Test
    void cannotConvertWithoutReady() {
        assertFalse(CheckoutStatus.CREATED.canTransitionTo(CheckoutStatus.CONVERTED));
        assertFalse(CheckoutStatus.ADDRESS_SELECTED.canTransitionTo(CheckoutStatus.CONVERTED));
        assertFalse(CheckoutStatus.PAYMENT_SELECTED.canTransitionTo(CheckoutStatus.CONVERTED));
    }
}