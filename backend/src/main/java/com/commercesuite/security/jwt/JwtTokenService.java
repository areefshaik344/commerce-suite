package com.commercesuite.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final JwtProperties props;
    private final SecretKey key;
    private final Clock clock;

    public JwtTokenService(JwtProperties props, Clock clock) {
        if (props.secret() == null || props.secret().isBlank())
            throw new IllegalStateException("JWT_SECRET env var is required and was not provided");
        byte[] secret = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32)
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes (got " + secret.length + ")");
        this.props = props;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public String issueAccessToken(UUID userId, Set<String> roles, Set<String> permissions) {
        Instant now = Instant.now(clock);
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(props.accessTtlMinutes() * 60)))
                .claim("typ", "access")
                .claim("roles", roles)
                .claim("perms", permissions)
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(key).requireIssuer(props.issuer()).build().parseSignedClaims(token);
    }

    public long refreshTtlSeconds() { return props.refreshTtlDays() * 24L * 3600L; }
}
