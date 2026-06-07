package com.commercesuite.orders.dto.storefront;

import java.time.Instant;

public record PaymentSummaryDto(
        String method,
        String status,
        MoneyDto amount,
        String gatewayReference,
        Instant paidAt
) {}