package com.commercesuite.mfa.controller;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.mfa.dto.MfaDtos.*;
import com.commercesuite.mfa.service.MfaService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/mfa")
@PreAuthorize("isAuthenticated()")
public class MfaController {
    private final MfaService mfa;
    public MfaController(MfaService mfa) { this.mfa = mfa; }

    @GetMapping("/status")
    public ApiResponse<StatusResponse> status(@AuthenticationPrincipal(expression = "id") UUID userId) {
        return ApiResponse.ok(new StatusResponse(mfa.isEnabled(userId)));
    }

    @PostMapping("/setup")
    public ApiResponse<SetupResponse> setup(@AuthenticationPrincipal(expression = "id") UUID userId,
                                            @RequestBody @Valid SetupRequest req) {
        var r = mfa.beginSetup(userId, req.accountLabel());
        return ApiResponse.ok(new SetupResponse(r.secret(), r.otpAuthUri()));
    }

    @PostMapping("/activate")
    public ApiResponse<ActivateResponse> activate(@AuthenticationPrincipal(expression = "id") UUID userId,
                                                  @RequestBody @Valid ActivateRequest req) {
        var r = mfa.activate(userId, req.code());
        return ApiResponse.ok(new ActivateResponse(r.recoveryCodes()));
    }

    @PostMapping("/verify")
    public ApiResponse<Boolean> verify(@AuthenticationPrincipal(expression = "id") UUID userId,
                                       @RequestBody @Valid VerifyRequest req) {
        return ApiResponse.ok(mfa.verify(userId, req.code()));
    }

    @DeleteMapping
    public ApiResponse<Void> disable(@AuthenticationPrincipal(expression = "id") UUID userId) {
        mfa.disable(userId); return ApiResponse.ok(null);
    }
}
