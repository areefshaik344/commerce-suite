package com.commercesuite.rbac;

import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.PermissionCatalog;
import com.commercesuite.rbac.service.Permissions;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PermissionCatalogTest {
    PermissionCatalog catalog = new PermissionCatalog();

    @Test void customerHasPlaceOrder() {
        assertTrue(catalog.permissionsFor(AppRole.CUSTOMER).contains(Permissions.PLACE_ORDER));
        assertFalse(catalog.permissionsFor(AppRole.CUSTOMER).contains(Permissions.MANAGE_PRODUCTS));
    }

    @Test void vendorHasProductMgmt() {
        assertTrue(catalog.permissionsFor(AppRole.VENDOR).contains(Permissions.MANAGE_PRODUCTS));
        assertFalse(catalog.permissionsFor(AppRole.VENDOR).contains(Permissions.APPLY_AS_VENDOR));
    }

    @Test void onlySuperAdminCanImpersonate() {
        assertTrue (catalog.permissionsFor(AppRole.SUPER_ADMIN).contains(Permissions.IMPERSONATE_USER));
        assertFalse(catalog.permissionsFor(AppRole.ADMIN      ).contains(Permissions.IMPERSONATE_USER));
    }

    @Test void multiRoleUnion() {
        var perms = catalog.effectivePermissions(List.of(AppRole.CUSTOMER, AppRole.VENDOR), Set.of());
        assertTrue(perms.contains(Permissions.PLACE_ORDER));
        assertTrue(perms.contains(Permissions.MANAGE_PRODUCTS));
    }

    @Test void overridesApplied() {
        var perms = catalog.effectivePermissions(List.of(AppRole.CUSTOMER), Set.of("CUSTOM_CAP"));
        assertTrue(perms.contains("CUSTOM_CAP"));
    }
}
