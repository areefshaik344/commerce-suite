package com.commercesuite.common.audit.log;

/** Canonical audit action vocabulary. New values must be appended (never re-ordered). */
public enum AuditAction {
    // Auth
    USER_REGISTERED, USER_LOGGED_IN, USER_LOGGED_OUT,
    PASSWORD_CHANGED, PASSWORD_RESET_REQUESTED, PASSWORD_RESET_COMPLETED,
    EMAIL_VERIFIED, REFRESH_TOKEN_REUSED,
    // Vendor / catalog
    VENDOR_APPROVED, VENDOR_REJECTED, VENDOR_SUSPENDED,
    PRODUCT_APPROVED, PRODUCT_REJECTED,
    // Inventory
    INVENTORY_ADJUSTED,
    // Orders / refunds
    ORDER_CREATED, ORDER_CANCELLED,
    REFUND_APPROVED, REFUND_REJECTED,
    // Settlement / payout
    SETTLEMENT_LOCKED, PAYOUT_COMPLETED,
    // Misc
    SECURITY_VIOLATION, ADMIN_OVERRIDE
}