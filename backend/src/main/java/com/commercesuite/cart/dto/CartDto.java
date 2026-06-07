package com.commercesuite.cart.dto;

import com.commercesuite.cart.entity.Cart;
import com.commercesuite.cart.entity.CartStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartDto(
        UUID id, UUID userId, CartStatus status, String currency,
        long subtotalPaise, int totalItems, List<CartItemDto> items,
        Instant lastActivityAt, Instant updatedAt) {
    public static CartDto from(Cart c, List<CartItemDto> items) {
        long subtotal = 0;
        int qty = 0;
        for (CartItemDto i : items) {
            subtotal = Math.addExact(subtotal, i.lineTotalPaise());
            qty += i.qty();
        }
        return new CartDto(c.getId(), c.getUserId(), c.getStatus(), c.getCurrency(),
                subtotal, qty, items, c.getLastActivityAt(), c.getUpdatedAt());
    }
}