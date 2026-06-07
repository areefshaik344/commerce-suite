package com.commercesuite.auth.controller;

import com.commercesuite.auth.dto.*;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.audit.ActorContextHolder;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;
    private final ActorContextHolder actorHolder;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponse>> signup(@Valid @RequestBody SignupRequest req, HttpServletRequest http) {
        var r = auth.signup(req, http.getHeader("User-Agent"), clientIp(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(r.tokens(), "Account created"));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return ApiResponse.ok(auth.login(req, http.getHeader("User-Agent"), clientIp(http)).tokens());
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest http) {
        return ApiResponse.ok(auth.refresh(req.refreshToken(), http.getHeader("User-Agent"), clientIp(http)));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req.refreshToken());
        return ApiResponse.ok(null, "Logged out");
    }

    @PostMapping("/logout/all")
    public ApiResponse<Map<String, Integer>> logoutAll() {
        int n = auth.logoutAll(actorHolder.require().userId());
        return ApiResponse.ok(Map.of("revoked", n), "All sessions revoked");
    }

    @PostMapping("/email/verify")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        auth.verifyEmail(req.token());
        return ApiResponse.ok(null, "Email verified");
    }

    @PostMapping("/password/forgot")
    public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        auth.forgotPassword(req.email());
        return ApiResponse.ok(null, "If the email exists, a reset link has been sent");
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordRequest req) {
        auth.resetPassword(req.token(), req.newPassword());
        return ApiResponse.ok(null, "Password reset");
    }

    @PostMapping("/password/change")
    public ApiResponse<Void> change(@Valid @RequestBody ChangePasswordRequest req) {
        auth.changePassword(actorHolder.require().userId(), req);
        return ApiResponse.ok(null, "Password changed");
    }

    private static String clientIp(HttpServletRequest r) {
        String xff = r.getHeader("X-Forwarded-For");
        return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : r.getRemoteAddr();
    }
}
