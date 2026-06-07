package com.commercesuite.checkout;

import static org.junit.jupiter.api.Assertions.*;

import com.commercesuite.AbstractIT;
import com.commercesuite.checkout.entity.CheckoutStatus;
import org.junit.jupiter.api.Test;

/**
 * Minimal context-load smoke test for the cart + checkout module.
 * Full end-to-end scenarios are exercised via service unit tests +
 * the FSM tests in this package.
 */
class CheckoutIT extends AbstractIT {

    @Test
    void contextLoads() {
        assertNotNull(CheckoutStatus.CREATED);
    }
}