package com.commercesuite.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID variantId,
        @NotNull @Min(1) Integer qty) {}
