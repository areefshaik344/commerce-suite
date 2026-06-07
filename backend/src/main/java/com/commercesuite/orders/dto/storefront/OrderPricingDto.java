package com.commercesuite.orders.dto.storefront;

public record OrderPricingDto(
        MoneyDto subtotal,
        MoneyDto discount,
        MoneyDto couponDiscount,
        MoneyDto shipping,
        MoneyDto tax,
        MoneyDto platformFee,
        MoneyDto grandTotal,
        String couponCode
) {}