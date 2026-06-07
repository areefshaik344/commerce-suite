package com.commercesuite.rbac.service;

/** String constants mirroring src/lib/permissions.ts PERMISSIONS. */
public final class Permissions {
    private Permissions() {}
    public static final String PLACE_ORDER          = "PLACE_ORDER";
    public static final String WRITE_REVIEW         = "WRITE_REVIEW";
    public static final String MANAGE_OWN_PROFILE   = "MANAGE_OWN_PROFILE";
    public static final String MANAGE_OWN_ADDRESSES = "MANAGE_OWN_ADDRESSES";
    public static final String USE_WISHLIST         = "USE_WISHLIST";
    public static final String APPLY_AS_VENDOR      = "APPLY_AS_VENDOR";

    public static final String MANAGE_PRODUCTS         = "MANAGE_PRODUCTS";
    public static final String MANAGE_INVENTORY        = "MANAGE_INVENTORY";
    public static final String MANAGE_VENDOR_PROFILE   = "MANAGE_VENDOR_PROFILE";
    public static final String VIEW_VENDOR_PAYOUTS     = "VIEW_VENDOR_PAYOUTS";
    public static final String MANAGE_VENDOR_ORDERS    = "MANAGE_VENDOR_ORDERS";
    public static final String MANAGE_VENDOR_RETURNS   = "MANAGE_VENDOR_RETURNS";
    public static final String MANAGE_VENDOR_COUPONS   = "MANAGE_VENDOR_COUPONS";
    public static final String MANAGE_VENDOR_ADS       = "MANAGE_VENDOR_ADS";
    public static final String MANAGE_STORE_PROFILE    = "MANAGE_STORE_PROFILE";
    public static final String VIEW_VENDOR_ANALYTICS   = "VIEW_VENDOR_ANALYTICS";
    public static final String VIEW_VENDOR_FINANCIALS  = "VIEW_VENDOR_FINANCIALS";
    public static final String RESPOND_TO_REVIEWS      = "RESPOND_TO_REVIEWS";
    public static final String MANAGE_DISPUTES         = "MANAGE_DISPUTES";

    public static final String MANAGE_USERS                  = "MANAGE_USERS";
    public static final String MANAGE_VENDORS                = "MANAGE_VENDORS";
    public static final String APPROVE_VENDOR_APPLICATIONS   = "APPROVE_VENDOR_APPLICATIONS";
    public static final String MODERATE_PRODUCTS             = "MODERATE_PRODUCTS";
    public static final String MODERATE_REVIEWS              = "MODERATE_REVIEWS";
    public static final String MANAGE_CATEGORIES             = "MANAGE_CATEGORIES";
    public static final String MANAGE_GLOBAL_COUPONS         = "MANAGE_GLOBAL_COUPONS";
    public static final String MANAGE_CMS                    = "MANAGE_CMS";
    public static final String MANAGE_EMAIL_TEMPLATES        = "MANAGE_EMAIL_TEMPLATES";
    public static final String MANAGE_PLATFORM_SETTINGS      = "MANAGE_PLATFORM_SETTINGS";
    public static final String VIEW_PLATFORM_ANALYTICS       = "VIEW_PLATFORM_ANALYTICS";
    public static final String VIEW_AUDIT_LOG                = "VIEW_AUDIT_LOG";
    public static final String MANAGE_FRAUD                  = "MANAGE_FRAUD";
    public static final String MANAGE_PAYOUTS                = "MANAGE_PAYOUTS";
    public static final String MANAGE_COMMISSIONS            = "MANAGE_COMMISSIONS";
    public static final String HANDLE_TICKETS                = "HANDLE_TICKETS";
    public static final String IMPERSONATE_USER              = "IMPERSONATE_USER";
}
