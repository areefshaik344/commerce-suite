package com.commercesuite.auth.service;

import com.commercesuite.auth.entity.RefreshToken;
import com.commercesuite.auth.repository.RefreshTokenRepository;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.security.jwt.JwtTokenService;
import com.commercesuite.security.service.HashUtil;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository repo;
    private final JwtTokenService jwt;
    private final Clock clock;

    public record IssuedRefresh(String rawToken, RefreshToken entity) {}

    @Transactional
    public IssuedRefresh issueNew(UUID userId, String userAgent, String ip) {
        String raw = HashUtil.randomToken(48);
        Instant now = Instant.now(clock);
        RefreshToken rt = RefreshToken.builder()
                .userId(userId)
                .tokenHash(HashUtil.sha256(raw))
                .familyId(UUID.randomUUID())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(jwt.refreshTtlSeconds()))
                .userAgent(truncate(userAgent, 255))
                .ipAddress(truncate(ip, 45))
                .build();
        repo.save(rt);
        return new IssuedRefresh(raw, rt);
    }

    @Transactional
    public IssuedRefresh rotate(String rawPresented, String userAgent, String ip) {
        String hash = HashUtil.sha256(rawPresented);
        RefreshToken current = repo.findByTokenHash(hash)
                .orElseThrow(() -> AppException.unauthorized(ErrorCode.TOKEN_INVALID, "Invalid refresh token"));

        Instant now = Instant.now(clock);
        if (current.getRevokedAt() != null) {
            repo.revokeFamily(current.getFamilyId(), now);
            throw AppException.unauthorized(ErrorCode.REFRESH_REUSE_DETECTED, "Refresh token reuse detected");
        }
        if (!current.isActive(now))
            throw AppException.unauthorized(ErrorCode.TOKEN_EXPIRED, "Refresh token expired");

        current.setRevokedAt(now);

        String raw = HashUtil.randomToken(48);
        RefreshToken next = RefreshToken.builder()
                .userId(current.getUserId())
                .tokenHash(HashUtil.sha256(raw))
                .familyId(current.getFamilyId())
                .parentId(current.getId())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(jwt.refreshTtlSeconds()))
                .userAgent(truncate(userAgent, 255))
                .ipAddress(truncate(ip, 45))
                .build();
        repo.save(next);
        return new IssuedRefresh(raw, next);
    }

    @Transactional
    public void revoke(String rawPresented) {
        repo.findByTokenHash(HashUtil.sha256(rawPresented))
                .ifPresent(rt -> { if (rt.getRevokedAt() == null) rt.setRevokedAt(Instant.now(clock)); });
    }

    @Transactional
    public int revokeAllForUser(UUID userId) { return repo.revokeAllForUser(userId, Instant.now(clock)); }

    private static String truncate(String v, int max) { return v == null ? null : v.length() > max ? v.substring(0, max) : v; }
}
