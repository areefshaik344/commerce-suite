package com.commercesuite.vendor.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** FSM for vendor lifecycle. See docs/VENDOR_MODULE.md. */
public enum VendorStatus {
    PENDING_APPLICATION,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    SUSPENDED,
    DEACTIVATED;

    private static final Map<VendorStatus, Set<VendorStatus>> ALLOWED = Map.of(
            PENDING_APPLICATION, EnumSet.of(UNDER_REVIEW, DEACTIVATED),
            UNDER_REVIEW,        EnumSet.of(APPROVED, REJECTED, DEACTIVATED),
            APPROVED,            EnumSet.of(SUSPENDED, DEACTIVATED),
            REJECTED,            EnumSet.of(UNDER_REVIEW, DEACTIVATED), // reapply
            SUSPENDED,           EnumSet.of(APPROVED, DEACTIVATED),
            DEACTIVATED,         EnumSet.noneOf(VendorStatus.class)
    );

    public boolean canTransitionTo(VendorStatus next) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(next);
    }
    public boolean isTerminal()   { return this == DEACTIVATED; }
    public boolean isOperational(){ return this == APPROVED; }
}