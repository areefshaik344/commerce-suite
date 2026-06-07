package com.commercesuite.security;

import com.commercesuite.common.exception.AppException;
import com.commercesuite.security.service.PasswordPolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    PasswordPolicy p = new PasswordPolicy();

    @Test void rejectsShort()      { assertThrows(AppException.class, () -> p.validate("Aa1!aa")); }
    @Test void rejectsNoSymbol()   { assertThrows(AppException.class, () -> p.validate("Abcdef12")); }
    @Test void rejectsNoUpper()    { assertThrows(AppException.class, () -> p.validate("abcdef1!")); }
    @Test void rejectsNoDigit()    { assertThrows(AppException.class, () -> p.validate("Abcdefg!")); }
    @Test void acceptsStrong()     { assertDoesNotThrow(() -> p.validate("Str0ng!Pwd")); }
    @Test void rejectsNull()       { assertThrows(AppException.class, () -> p.validate(null)); }
}
