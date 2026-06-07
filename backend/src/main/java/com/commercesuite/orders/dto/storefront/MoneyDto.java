package com.commercesuite.orders.dto.storefront;

/** Money in paise plus ISO currency. Frontend converts to rupees. */
public record MoneyDto(long amountPaise, String currency) {
    public static MoneyDto of(long paise, String currency) {
        return new MoneyDto(paise, currency == null ? "INR" : currency);
    }
}