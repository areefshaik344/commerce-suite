package com.commercesuite.common.audit;

import com.commercesuite.common.exception.AppException;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.security.jwt.JwtAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ActorContextHolder {
    public ActorContext current() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a instanceof JwtAuthenticationToken jwt) return jwt.actor();
        return null;
    }
    public ActorContext require() {
        ActorContext a = current();
        if (a == null || !a.isAuthenticated())
            throw AppException.unauthorized(ErrorCode.UNAUTHENTICATED, "Authentication required");
        return a;
    }
}
