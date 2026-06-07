package com.commercesuite.vendor;

import com.commercesuite.vendor.entity.VendorStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VendorStateMachineTest {
    @Test void legalTransitions() {
        assertTrue(VendorStatus.PENDING_APPLICATION.canTransitionTo(VendorStatus.UNDER_REVIEW));
        assertTrue(VendorStatus.UNDER_REVIEW.canTransitionTo(VendorStatus.APPROVED));
        assertTrue(VendorStatus.UNDER_REVIEW.canTransitionTo(VendorStatus.REJECTED));
        assertTrue(VendorStatus.APPROVED.canTransitionTo(VendorStatus.SUSPENDED));
        assertTrue(VendorStatus.SUSPENDED.canTransitionTo(VendorStatus.APPROVED));
        assertTrue(VendorStatus.REJECTED.canTransitionTo(VendorStatus.UNDER_REVIEW));   // reapply
    }
    @Test void illegalTransitions() {
        assertFalse(VendorStatus.PENDING_APPLICATION.canTransitionTo(VendorStatus.APPROVED));
        assertFalse(VendorStatus.APPROVED.canTransitionTo(VendorStatus.UNDER_REVIEW));
        assertFalse(VendorStatus.DEACTIVATED.canTransitionTo(VendorStatus.APPROVED));
        assertFalse(VendorStatus.REJECTED.canTransitionTo(VendorStatus.APPROVED));
    }
    @Test void terminality() {
        assertTrue(VendorStatus.DEACTIVATED.isTerminal());
        assertFalse(VendorStatus.APPROVED.isTerminal());
    }
}