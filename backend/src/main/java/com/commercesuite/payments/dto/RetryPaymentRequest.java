package com.commercesuite.payments.dto;
import jakarta.validation.constraints.Size;

public record RetryPaymentRequest(
    @Size(max=64) String gatewayProvider,
    @Size(max=64) String simulateOutcome // SUCCESS|FAILURE — Phase 7 sandbox
) {}