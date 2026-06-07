package com.commercesuite.user.entity;

/** Mirrors src/lib/accountStatus.ts AccountStatus. */
public enum AccountStatus {
    ACTIVE,
    PENDING_VERIFICATION,
    PENDING_VENDOR_APPROVAL,
    SUSPENDED,
    BANNED,
    DEACTIVATED;

    public boolean isActionable() {
        return this != SUSPENDED && this != BANNED && this != DEACTIVATED;
    }
    public boolean canLogin() {
        return this != BANNED && this != DEACTIVATED;
    }
}
