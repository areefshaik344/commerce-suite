package com.commercesuite.auth;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.entity.RefreshToken;
import com.commercesuite.auth.repository.RefreshTokenRepository;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.auth.dto.SignupRequest;
import com.commercesuite.security.service.HashUtil;
import com.commercesuite.rbac.entity.AppRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenRotationIT extends AbstractIT {

    @Autowired AuthService auth;
    @Autowired RefreshTokenRepository tokens;

    @Test
    void successfulRotationRevokesOldAndIssuesNewInSameFamily() {
        var signup = auth.signup(new SignupRequest("rot1+" + UUID.randomUUID() + "@example.com",
                "Str0ng!Pwd", "Rot1", null, AppRole.CUSTOMER), "ua", "127.0.0.1");
        String original = signup.tokens().refreshToken();

        var newTokens = auth.refresh(original, "ua", "127.0.0.1");
        assertNotEquals(original, newTokens.refreshToken());

        RefreshToken old = tokens.findByTokenHash(HashUtil.sha256(original)).orElseThrow();
        RefreshToken next = tokens.findByTokenHash(HashUtil.sha256(newTokens.refreshToken())).orElseThrow();
        assertNotNull(old.getRevokedAt(), "old token must be revoked");
        assertNull(next.getRevokedAt(), "new token must be active");
        assertEquals(old.getFamilyId(), next.getFamilyId(), "family preserved");
        assertEquals(old.getId(), next.getParentId(), "parent linked");
    }

    @Test
    void reuseOfRevokedTokenRevokesEntireFamily() {
        var signup = auth.signup(new SignupRequest("rot2+" + UUID.randomUUID() + "@example.com",
                "Str0ng!Pwd", "Rot2", null, AppRole.CUSTOMER), "ua", "127.0.0.1");
        String original = signup.tokens().refreshToken();
        var rotated = auth.refresh(original, "ua", "127.0.0.1");

        Exception ex = assertThrows(Exception.class, () -> auth.refresh(original, "ua", "127.0.0.1"));
        assertTrue(ex.getMessage().toLowerCase().contains("reuse")
                || ex.getMessage().toLowerCase().contains("invalid"));

        // The active token issued by the legitimate rotation must now be revoked.
        RefreshToken next = tokens.findByTokenHash(HashUtil.sha256(rotated.refreshToken())).orElseThrow();
        assertNotNull(next.getRevokedAt(), "family revoke must invalidate active sibling");
    }
}
