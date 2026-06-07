package com.commercesuite.vendor;

import com.commercesuite.auth.dto.SignupRequest;
import com.commercesuite.auth.dto.LoginRequest;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.vendor.dto.ApplyVendorRequest;
import java.util.UUID;

public final class VendorTestSupport {
    private VendorTestSupport() {}

    public record TestUser(String email, String token, UUID userId) {}

    public static TestUser signupCustomer(AuthService auth, String prefix) {
        String email = prefix + "+" + UUID.randomUUID() + "@example.com";
        var res = auth.signup(new SignupRequest(email, "Str0ng!Pwd", prefix, null, AppRole.CUSTOMER),
                "ua", "127.0.0.1");
        return new TestUser(email, res.tokens().accessToken(), res.userId());
    }

    /** Register a customer, promote to ADMIN, then re-login so the JWT carries the ADMIN role. */
    public static TestUser signupAdmin(AuthService auth, RoleService roles, String prefix) {
        TestUser u = signupCustomer(auth, prefix);
        roles.grant(u.userId(), AppRole.ADMIN, null);
        var login = auth.login(new LoginRequest(u.email(), "Str0ng!Pwd"), "ua", "127.0.0.1");
        return new TestUser(u.email(), login.tokens().accessToken(), u.userId());
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