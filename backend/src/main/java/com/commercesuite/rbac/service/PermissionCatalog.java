package com.commercesuite.rbac.service;

import com.commercesuite.rbac.entity.AppRole;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.commercesuite.rbac.service.Permissions.*;

/** Authoritative role -> permission mapping. Mirrors ROLE_PERMISSIONS in src/lib/permissions.ts. */
@Component
public class PermissionCatalog {
    private final Map<AppRole, Set<String>> rolePerms = new EnumMap<>(AppRole.class);

    public PermissionCatalog() {
        Set<String> customer = Set.of(PLACE_ORDER, WRITE_REVIEW, MANAGE_OWN_PROFILE,
                MANAGE_OWN_ADDRESSES, USE_WISHLIST, APPLY_AS_VENDOR);

        Set<String> vendor = union(without(customer, APPLY_AS_VENDOR),
                MANAGE_PRODUCTS, MANAGE_INVENTORY, MANAGE_VENDOR_ORDERS, MANAGE_VENDOR_RETURNS,
                MANAGE_VENDOR_COUPONS, MANAGE_VENDOR_ADS, MANAGE_STORE_PROFILE,
                MANAGE_VENDOR_PROFILE, VIEW_VENDOR_PAYOUTS,
                VIEW_VENDOR_ANALYTICS, VIEW_VENDOR_FINANCIALS, RESPOND_TO_REVIEWS, MANAGE_DISPUTES);

        Set<String> admin = union(without(customer, APPLY_AS_VENDOR),
                MANAGE_USERS, MANAGE_VENDORS, APPROVE_VENDOR_APPLICATIONS,
                MODERATE_PRODUCTS, MODERATE_REVIEWS, MANAGE_CATEGORIES, MANAGE_GLOBAL_COUPONS,
                MANAGE_CMS, MANAGE_EMAIL_TEMPLATES, MANAGE_PLATFORM_SETTINGS,
                VIEW_PLATFORM_ANALYTICS, VIEW_AUDIT_LOG, MANAGE_FRAUD,
                MANAGE_PAYOUTS, MANAGE_COMMISSIONS, HANDLE_TICKETS);

        rolePerms.put(AppRole.CUSTOMER, customer);
        rolePerms.put(AppRole.VENDOR,   vendor);
        rolePerms.put(AppRole.ADMIN,    admin);
        rolePerms.put(AppRole.SUPER_ADMIN, union(admin, IMPERSONATE_USER));
        rolePerms.put(AppRole.SUPPORT_ADMIN, Set.of(MANAGE_OWN_PROFILE, HANDLE_TICKETS, MANAGE_USERS, MANAGE_DISPUTES));
        rolePerms.put(AppRole.MODERATOR, Set.of(MANAGE_OWN_PROFILE, MODERATE_PRODUCTS, MODERATE_REVIEWS, HANDLE_TICKETS));
        rolePerms.put(AppRole.FINANCE_ADMIN, Set.of(MANAGE_OWN_PROFILE, MANAGE_PAYOUTS, MANAGE_COMMISSIONS,
                VIEW_PLATFORM_ANALYTICS, VIEW_AUDIT_LOG));
    }

    public Set<String> permissionsFor(AppRole role) { return rolePerms.getOrDefault(role, Set.of()); }

    public Set<String> effectivePermissions(Collection<AppRole> roles, Collection<String> overrides) {
        Set<String> out = new HashSet<>();
        for (AppRole r : roles) out.addAll(permissionsFor(r));
        if (overrides != null) out.addAll(overrides);
        return out;
    }

    private static Set<String> union(Set<String> base, String... extra) {
        Set<String> s = new HashSet<>(base); Collections.addAll(s, extra); return s;
    }
    private static Set<String> without(Set<String> base, String item) {
        Set<String> s = new HashSet<>(base); s.remove(item); return s;
    }
}
