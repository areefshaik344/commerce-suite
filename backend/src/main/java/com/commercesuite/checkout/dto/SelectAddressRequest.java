package com.commercesuite.checkout.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SelectAddressRequest(@NotNull UUID addressId) {}