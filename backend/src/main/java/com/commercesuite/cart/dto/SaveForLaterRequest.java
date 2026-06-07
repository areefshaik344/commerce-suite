package com.commercesuite.cart.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SaveForLaterRequest(@NotNull UUID cartItemId) {}