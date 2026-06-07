package com.commercesuite.common.ratelimit;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {
    @Test void allowsUpToCapacityThenBlocks() {
        RateLimitService s = new RateLimitService();
        RateLimitPolicy p = RateLimitPolicy.of("t", 3, 60);
        assertTrue(s.tryAcquire("k", p));
        assertTrue(s.tryAcquire("k", p));
        assertTrue(s.tryAcquire("k", p));
        assertFalse(s.tryAcquire("k", p));
    }
    @Test void differentKeysAreIndependent() {
        RateLimitService s = new RateLimitService();
        RateLimitPolicy p = RateLimitPolicy.of("t", 1, 60);
        assertTrue(s.tryAcquire("a", p));
        assertTrue(s.tryAcquire("b", p));
        assertFalse(s.tryAcquire("a", p));
    }
}
