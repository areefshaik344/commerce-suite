package com.commercesuite.mfa.service;

import java.util.Set;
import org.springframework.stereotype.Component;

/** Roles for which MFA is mandatory at login (admin + finance). */
@Component
public class MfaEnforcement {
    public static final Set<String> MANDATORY_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "FINANCE");
    public boolean isMandatoryForRole(String role) { return role != null && MANDATORY_ROLES.contains(role); }
}
