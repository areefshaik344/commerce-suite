package com.commercesuite.orders.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record CreateOrderRequest(@NotNull UUID checkoutId) {}
