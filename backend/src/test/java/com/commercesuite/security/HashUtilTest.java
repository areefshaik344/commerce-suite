package com.commercesuite.security;

import com.commercesuite.security.service.HashUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HashUtilTest {
    @Test void sha256IsStable() {
        assertEquals(HashUtil.sha256("hello"), HashUtil.sha256("hello"));
        assertNotEquals(HashUtil.sha256("hello"), HashUtil.sha256("hello!"));
    }
    @Test void randomTokensDiffer() {
        assertNotEquals(HashUtil.randomToken(32), HashUtil.randomToken(32));
    }
}
