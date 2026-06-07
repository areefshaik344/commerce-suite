package com.commercesuite.common.idempotency;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit test for the idempotency key validator + TTL contract. The
 * full persistence/replay flow is covered by the Phase 7 integration
 * tests once Testcontainers is available.
 */
class IdempotencyServiceTest {
    @Test void ttlIsTwentyFourHours() {
        assertEquals(24, IdempotencyService.TTL.toHours());
    }
    @Test void keyFormatValidator() {
        assertTrue(com.commercesuite.common.util.IdempotencyKey.isValid("orders:11111111-2222-3333-4444-555555555555"));
        assertFalse(com.commercesuite.common.util.IdempotencyKey.isValid("short"));
        assertFalse(com.commercesuite.common.util.IdempotencyKey.isValid(null));
    }
}