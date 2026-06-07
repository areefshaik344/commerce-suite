package com.commercesuite.auth.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Canonical Auth domain events (Phase 8.1 / PHASE8_IMPLEMENTATION_BLUEPRINT.md).
 * All payloads serialised into the durable outbox via {@code OutboxPublisher}.
 * Aggregate type for every record below is {@code "USER"}.
 */
public final class AuthEvents {
    private AuthEvents() {}

    public static final String AGGREGATE = "USER";

    public static final String USER_REGISTERED            = "auth.user.registered";
    public static final String USER_LOGGED_IN             = "auth.user.logged_in";
    public static final String USER_LOGGED_OUT            = "auth.user.logged_out";
    public static final String PASSWORD_CHANGED           = "auth.password.changed";
    public static final String PASSWORD_RESET_REQUESTED   = "auth.password.reset_requested";
    public static final String PASSWORD_RESET_COMPLETED   = "auth.password.reset_completed";
    public static final String EMAIL_VERIFIED             = "auth.email.verified";
    public static final String REFRESH_TOKEN_REUSED       = "auth.refresh.reuse_detected";

    public record UserRegisteredPayload(UUID userId, String email, String role, Instant at) {}
    public record UserLoggedInPayload(UUID userId, String ip, String userAgent, Instant at) {}
    public record UserLoggedOutPayload(UUID userId, boolean allSessions, Instant at) {}
    public record PasswordChangedPayload(UUID userId, Instant at) {}
    public record PasswordResetRequestedPayload(UUID userId, Instant at) {}
    public record PasswordResetCompletedPayload(UUID userId, Instant at) {}
    public record EmailVerifiedPayload(UUID userId, Instant at) {}
    public record RefreshTokenReusedPayload(UUID userId, UUID familyId, String ip, Instant at) {}
}