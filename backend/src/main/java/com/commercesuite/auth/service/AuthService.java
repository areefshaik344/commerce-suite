package com.commercesuite.auth.service;

import com.commercesuite.auth.dto.*;
import com.commercesuite.auth.entity.EmailVerificationToken;
import com.commercesuite.auth.entity.PasswordResetToken;
import com.commercesuite.auth.repository.EmailVerificationTokenRepository;
import com.commercesuite.auth.repository.PasswordResetTokenRepository;
import com.commercesuite.auth.event.AuthEvents;
import com.commercesuite.common.outbox.OutboxPublisher;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.security.jwt.JwtTokenService;
import com.commercesuite.security.service.HashUtil;
import com.commercesuite.security.service.PasswordPolicy;
import com.commercesuite.user.entity.AccountStatus;
import com.commercesuite.user.entity.Profile;
import com.commercesuite.user.entity.User;
import com.commercesuite.user.repository.ProfileRepository;
import com.commercesuite.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int  MAX_FAILED   = 5;
    private static final int  LOCK_MINUTES = 15;
    private static final long EMAIL_TOKEN_HOURS  = 24;
    private static final long RESET_TOKEN_MINUTES = 30;

    private final UserRepository userRepo;
    private final ProfileRepository profileRepo;
    private final RoleService roleService;
    private final PasswordEncoder encoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtTokenService jwt;
    private final RefreshTokenService refreshService;
    private final AccountStatusGuard statusGuard;
    private final EmailVerificationTokenRepository evtRepo;
    private final PasswordResetTokenRepository prtRepo;
    private final Clock clock;
    private final OutboxPublisher outbox;

    @Transactional
    public AuthResult signup(SignupRequest req, String userAgent, String ip) {
        AppRole role = req.requestedRole() == null ? AppRole.CUSTOMER : req.requestedRole();
        if (!AppRole.isSelfRegistrable(role))
            throw AppException.forbidden("Admin accounts cannot self-register");

        passwordPolicy.validate(req.password());
        String email = req.email().trim().toLowerCase();
        if (userRepo.existsByEmailIgnoreCase(email))
            throw AppException.conflict(ErrorCode.EMAIL_TAKEN, "Email already in use");
        if (req.phone() != null && userRepo.existsByPhone(req.phone()))
            throw AppException.conflict(ErrorCode.PHONE_TAKEN, "Phone already in use");

        User u = userRepo.save(User.builder()
                .email(email)
                .phone(req.phone())
                .passwordHash(encoder.encode(req.password()))
                .accountStatus(role == AppRole.VENDOR
                        ? AccountStatus.PENDING_VENDOR_APPROVAL
                        : AccountStatus.PENDING_VERIFICATION)
                .build());

        profileRepo.save(Profile.builder().userId(u.getId()).fullName(req.fullName()).build());
        roleService.grant(u.getId(), role, null);
        if (role == AppRole.VENDOR) roleService.grant(u.getId(), AppRole.CUSTOMER, null);

        String verifyToken = issueEmailVerificationToken(u.getId());
        log.info("[signup] user={} role={} verifyTokenIssued", u.getId(), role);
        outbox.publish(AuthEvents.AGGREGATE, u.getId().toString(), AuthEvents.USER_REGISTERED,
                new AuthEvents.UserRegisteredPayload(u.getId(), u.getEmail(), role.name(), Instant.now(clock)));
        return issueTokens(u, userAgent, ip, verifyToken);
    }

    @Transactional
    public AuthResult login(LoginRequest req, String userAgent, String ip) {
        User u = userRepo.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> AppException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials"));
        statusGuard.assertCanLogin(u);
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            u.setFailedLoginCount(u.getFailedLoginCount() + 1);
            if (u.getFailedLoginCount() >= MAX_FAILED)
                u.setLockedUntil(Instant.now(clock).plus(LOCK_MINUTES, ChronoUnit.MINUTES));
            throw AppException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        u.setLastLoginAt(Instant.now(clock));
        outbox.publish(AuthEvents.AGGREGATE, u.getId().toString(), AuthEvents.USER_LOGGED_IN,
                new AuthEvents.UserLoggedInPayload(u.getId(), ip, userAgent, Instant.now(clock)));
        return issueTokens(u, userAgent, ip, null);
    }

    @Transactional
    public TokenResponse refresh(String rawRefresh, String userAgent, String ip) {
        var rotated = refreshService.rotate(rawRefresh, userAgent, ip);
        User u = userRepo.findById(rotated.entity().getUserId())
                .orElseThrow(() -> AppException.unauthorized(ErrorCode.TOKEN_INVALID, "User missing"));
        statusGuard.assertCanLogin(u);
        return buildTokenResponse(u, rotated.rawToken());
    }

    @Transactional public void logout(String rawRefresh) {
        var maybe = refreshService.revoke(rawRefresh);
        maybe.ifPresent(uid -> outbox.publish(AuthEvents.AGGREGATE, uid.toString(),
                AuthEvents.USER_LOGGED_OUT,
                new AuthEvents.UserLoggedOutPayload(uid, false, Instant.now(clock))));
    }
    @Transactional public int  logoutAll(UUID userId)     {
        int n = refreshService.revokeAllForUser(userId);
        outbox.publish(AuthEvents.AGGREGATE, userId.toString(), AuthEvents.USER_LOGGED_OUT,
                new AuthEvents.UserLoggedOutPayload(userId, true, Instant.now(clock)));
        return n;
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken t = evtRepo.findByTokenHash(HashUtil.sha256(rawToken))
                .orElseThrow(() -> AppException.badRequest(ErrorCode.TOKEN_INVALID, "Invalid verification token"));
        Instant now = Instant.now(clock);
        if (t.getConsumedAt() != null) throw AppException.badRequest(ErrorCode.TOKEN_INVALID, "Token already used");
        if (t.getExpiresAt().isBefore(now))
            throw AppException.badRequest(ErrorCode.TOKEN_EXPIRED, "Verification token expired");
        User u = userRepo.findById(t.getUserId()).orElseThrow(() -> AppException.notFound("User"));
        u.setEmailVerifiedAt(now);
        if (u.getAccountStatus() == AccountStatus.PENDING_VERIFICATION)
            u.setAccountStatus(AccountStatus.ACTIVE);
        t.setConsumedAt(now);
        outbox.publish(AuthEvents.AGGREGATE, u.getId().toString(), AuthEvents.EMAIL_VERIFIED,
                new AuthEvents.EmailVerifiedPayload(u.getId(), now));
    }

    public String issueEmailVerificationToken(UUID userId) {
        String raw = HashUtil.randomToken(32);
        evtRepo.save(EmailVerificationToken.builder()
                .userId(userId).tokenHash(HashUtil.sha256(raw))
                .createdAt(Instant.now(clock))
                .expiresAt(Instant.now(clock).plus(EMAIL_TOKEN_HOURS, ChronoUnit.HOURS)).build());
        return raw;
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepo.findByEmailIgnoreCase(email.trim()).ifPresent(u -> {
            String raw = HashUtil.randomToken(32);
            prtRepo.save(PasswordResetToken.builder()
                    .userId(u.getId()).tokenHash(HashUtil.sha256(raw))
                    .createdAt(Instant.now(clock))
                    .expiresAt(Instant.now(clock).plus(RESET_TOKEN_MINUTES, ChronoUnit.MINUTES)).build());
            log.info("[forgot-password] user={} resetTokenIssued", u.getId());
            outbox.publish(AuthEvents.AGGREGATE, u.getId().toString(),
                    AuthEvents.PASSWORD_RESET_REQUESTED,
                    new AuthEvents.PasswordResetRequestedPayload(u.getId(), Instant.now(clock)));
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        passwordPolicy.validate(newPassword);
        PasswordResetToken t = prtRepo.findByTokenHash(HashUtil.sha256(rawToken))
                .orElseThrow(() -> AppException.badRequest(ErrorCode.TOKEN_INVALID, "Invalid reset token"));
        Instant now = Instant.now(clock);
        if (t.getConsumedAt() != null) throw AppException.badRequest(ErrorCode.TOKEN_INVALID, "Token already used");
        if (t.getExpiresAt().isBefore(now))
            throw AppException.badRequest(ErrorCode.TOKEN_EXPIRED, "Reset token expired");
        User u = userRepo.findById(t.getUserId()).orElseThrow(() -> AppException.notFound("User"));
        u.setPasswordHash(encoder.encode(newPassword));
        t.setConsumedAt(now);
        refreshService.revokeAllForUser(u.getId());
        outbox.publish(AuthEvents.AGGREGATE, u.getId().toString(),
                AuthEvents.PASSWORD_RESET_COMPLETED,
                new AuthEvents.PasswordResetCompletedPayload(u.getId(), now));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        passwordPolicy.validate(req.newPassword());
        User u = userRepo.findById(userId).orElseThrow(() -> AppException.notFound("User"));
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash()))
            throw AppException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect");
        if (encoder.matches(req.newPassword(), u.getPasswordHash()))
            throw AppException.badRequest(ErrorCode.WEAK_PASSWORD, "New password must differ from current");
        u.setPasswordHash(encoder.encode(req.newPassword()));
        refreshService.revokeAllForUser(userId);
        outbox.publish(AuthEvents.AGGREGATE, userId.toString(), AuthEvents.PASSWORD_CHANGED,
                new AuthEvents.PasswordChangedPayload(userId, Instant.now(clock)));
    }

    public record AuthResult(TokenResponse tokens, UUID userId, String emailVerificationToken) {}

    private AuthResult issueTokens(User u, String userAgent, String ip, String verifyToken) {
        var rotated = refreshService.issueNew(u.getId(), userAgent, ip);
        return new AuthResult(buildTokenResponse(u, rotated.rawToken()), u.getId(), verifyToken);
    }

    private TokenResponse buildTokenResponse(User u, String rawRefresh) {
        Set<String> roleStrings = roleService.rolesOf(u.getId()).stream()
                .map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<String> perms = roleService.permissionsOf(u.getId());
        String access = jwt.issueAccessToken(u.getId(), roleStrings, perms);
        return TokenResponse.bearer(access, rawRefresh, jwt.refreshTtlSeconds());
    }
}
