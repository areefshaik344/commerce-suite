package com.commercesuite.settlement.dto;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CalculateSettlementRequest(@NotNull UUID vendorId,
                                         @NotNull Instant periodStart,
                                         @NotNull Instant periodEnd) {}
