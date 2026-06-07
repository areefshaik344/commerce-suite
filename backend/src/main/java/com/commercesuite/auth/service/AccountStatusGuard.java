package com.commercesuite.auth.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.user.entity.User;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AccountStatusGuard {
    public void assertCanLogin(User u) {
        if (u.getDeletedAt() != null)
            throw AppException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        if (!u.getAccountStatus().canLogin())
            throw AppException.unauthorized(ErrorCode.ACCOUNT_NOT_ACTIONABLE,
                "Account " + u.getAccountStatus());
        if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(Instant.now()))
            throw AppException.unauthorized(ErrorCode.ACCOUNT_LOCKED, "Account temporarily locked");
    }
}
