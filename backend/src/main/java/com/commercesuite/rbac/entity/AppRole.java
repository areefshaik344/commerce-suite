package com.commercesuite.rbac.entity;

/** Mirrors src/lib/permissions.ts AppRole. Stored in user_roles table only. */
public enum AppRole {
    CUSTOMER, VENDOR, ADMIN, SUPER_ADMIN, SUPPORT_ADMIN, MODERATOR, FINANCE_ADMIN;

    public static boolean isAdminRole(AppRole r) {
        return r == ADMIN || r == SUPER_ADMIN || r == SUPPORT_ADMIN || r == MODERATOR || r == FINANCE_ADMIN;
    }
    public static boolean isSelfRegistrable(AppRole r) { return r == CUSTOMER || r == VENDOR; }
}
