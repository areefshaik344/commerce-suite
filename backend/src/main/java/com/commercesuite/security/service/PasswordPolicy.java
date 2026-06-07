package com.commercesuite.security.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Conservative complexity policy. Frontend enforces UX, backend enforces correctness. */
@Component
public class PasswordPolicy {
    private static final Pattern HAS_LOWER  = Pattern.compile("[a-z]");
    private static final Pattern HAS_UPPER  = Pattern.compile("[A-Z]");
    private static final Pattern HAS_DIGIT  = Pattern.compile("\\d");
    private static final Pattern HAS_SYMBOL = Pattern.compile("[^A-Za-z0-9]");

    public void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 128
                || !HAS_LOWER.matcher(password).find()
                || !HAS_UPPER.matcher(password).find()
                || !HAS_DIGIT.matcher(password).find()
                || !HAS_SYMBOL.matcher(password).find()) {
            throw AppException.badRequest(ErrorCode.WEAK_PASSWORD,
                "Password must be 8-128 chars and include lower, upper, digit and symbol");
        }
    }
}
