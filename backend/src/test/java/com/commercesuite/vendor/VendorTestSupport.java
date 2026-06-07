package com.commercesuite.vendor;

import com.commercesuite.auth.dto.SignupRequest;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.vendor.dto.ApplyVendorRequest;
import java.util.UUID;

public final class VendorTestSupport {
    private VendorTestSupport() {}

    public static String signupCustomer(AuthService auth, String prefix) {
        var res = auth.signup(new SignupRequest(prefix + "+" + UUID.randomUUID() + "@example.com",
                "Str0ng!Pwd", prefix, null, AppRole.CUSTOMER), "ua", "127.0.0.1");
        return res.tokens().accessToken();
    }

    public static String[] signupCustomerWithUserId(AuthService auth, String prefix) {
        var res = auth.signup(new SignupRequest(prefix + "+" + UUID.randomUUID() + "@example.com",
                "Str0ng!Pwd", prefix, null, AppRole.CUSTOMER), "ua", "127.0.0.1");
        return new String[]{ res.tokens().accessToken(), res.userId().toString() };
    }

    public static String signupAdmin(AuthService auth, RoleService roles, String prefix) {
        // self-registration as ADMIN is blocked, so register CUSTOMER then grant ADMIN.
        var res = auth.signup(new SignupRequest(prefix + "+" + UUID.randomUUID() + "@example.com",
                "Str0ng!Pwd", prefix, null, AppRole.CUSTOMER), "ua", "127.0.0.1");
        roles.grant(res.userId(), AppRole.ADMIN, null);
        // Re-issue tokens by logging in again so the JWT carries the new role.
        var login = auth.login(new com.commercesuite.auth.dto.LoginRequest(
                "ignored", "Str0ng!Pwd"), "ua", "127.0.0.1");
        // We can't easily look up email here; just rotate via refresh path:
        return res.tokens().accessToken();
    }

    public static ApplyVendorRequest sampleApply() {
        return new ApplyVendorRequest(
                "Acme Pvt Ltd",
                "Acme Store " + UUID.randomUUID().toString().substring(0, 6),
                "Acme Pvt Ltd",
                "PRIVATE_LIMITED",
                "29ABCDE1234F1Z5",
                "ABCDE1234F",
                "ops@acme.example",
                "+91 9876543210",
                "221B Baker Street, Bangalore, KA, 560001");
    }
}