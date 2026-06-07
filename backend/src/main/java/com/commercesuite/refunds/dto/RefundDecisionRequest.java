package com.commercesuite.refunds.dto;
import jakarta.validation.constraints.Size;
public record RefundDecisionRequest(@Size(max=500) String reason) {}
