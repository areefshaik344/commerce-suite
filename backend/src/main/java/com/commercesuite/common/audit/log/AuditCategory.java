package com.commercesuite.common.audit.log;

/**
 * Functional grouping for audit records. Drives retention policy lookup,
 * coverage validation, and admin search filtering. Order is stable.
 */
public enum AuditCategory {
    AUTH,
    SECURITY,
    VENDOR,
    CATALOG,
    INVENTORY,
    ORDER,
    PAYMENT,
    REFUND,
    PAYOUT,
    SYSTEM,
    ADMIN
}