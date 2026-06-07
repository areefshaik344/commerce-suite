package com.commercesuite.auth.dto;

import com.commercesuite.rbac.entity.AppRole;
import jakarta.validation.constraints.*;

public record SignupRequest(
    @Email @NotBlank @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 128) String password,
    @Size(max = 120) String fullName,
    @Pattern(regexp = "^\\+?[0-9 \\-]{10,15}$") String phone,
    /** Only CUSTOMER or VENDOR may be requested; admin roles are provisioned by backend only. */
    AppRole requestedRole
) {}
