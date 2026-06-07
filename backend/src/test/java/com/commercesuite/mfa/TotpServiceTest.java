package com.commercesuite.mfa;

import static org.junit.jupiter.api.Assertions.*;
import com.commercesuite.mfa.service.TotpService;
import org.junit.jupiter.api.Test;

class TotpServiceTest {
    @Test void generatesAndVerifies() {
        TotpService t = new TotpService();
        String secret = t.generateSecret();
        long c = java.time.Instant.now().getEpochSecond() / 30;
        String code = t.generate(secret, c);
        assertEquals(6, code.length());
        assertTrue(t.verify(secret, code));
    }
    @Test void rejectsWrongCode() {
        TotpService t = new TotpService();
        assertFalse(t.verify(t.generateSecret(), "000000"));
    }
}
