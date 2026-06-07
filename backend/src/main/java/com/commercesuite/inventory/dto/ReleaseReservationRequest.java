package com.commercesuite.inventory.dto;

import com.commercesuite.inventory.entity.ReservationReleaseReason;
import jakarta.validation.constraints.NotNull;

public record ReleaseReservationRequest(@NotNull ReservationReleaseReason reason) {}