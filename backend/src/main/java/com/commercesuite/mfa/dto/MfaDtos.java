package com.commercesuite.mfa.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class MfaDtos {
    public record SetupRequest(@NotBlank String accountLabel) {}
    public record SetupResponse(String secret, String otpAuthUri) {}
    public record ActivateRequest(@NotBlank String code) {}
    public record ActivateResponse(List<String> recoveryCodes) {}
    public record VerifyRequest(@NotBlank String code) {}
    public record StatusResponse(boolean enabled) {}
}
