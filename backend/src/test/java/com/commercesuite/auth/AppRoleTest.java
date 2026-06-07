package com.commercesuite.auth;

import com.commercesuite.rbac.entity.AppRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppRoleTest {
    @Test void onlyCustomerAndVendorSelfRegister() {
        assertTrue (AppRole.isSelfRegistrable(AppRole.CUSTOMER));
        assertTrue (AppRole.isSelfRegistrable(AppRole.VENDOR));
        for (AppRole r : AppRole.values())
            if (r != AppRole.CUSTOMER && r != AppRole.VENDOR)
                assertFalse(AppRole.isSelfRegistrable(r), r.name());
    }

    @Test void adminRoleDetection() {
        assertTrue(AppRole.isAdminRole(AppRole.SUPER_ADMIN));
        assertTrue(AppRole.isAdminRole(AppRole.MODERATOR));
        assertFalse(AppRole.isAdminRole(AppRole.CUSTOMER));
    }
}
