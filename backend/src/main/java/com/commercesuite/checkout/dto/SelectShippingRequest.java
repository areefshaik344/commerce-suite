package com.commercesuite.checkout.dto;

import com.commercesuite.checkout.entity.ShippingMethodKind;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SelectShippingRequest(
        @NotNull ShippingMethodKind method,
        @NotNull @Min(0) Long shippingAmountPaise) {}