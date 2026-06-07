package com.commercesuite.orders.dto.storefront;

import java.util.UUID;

public record OrderLineItemDto(
        UUID id,
        UUID productId,
        UUID variantId,
        UUID vendorId,
        String vendorName,
        String productTitle,
        String productSlug,
        String imageUrl,
        String sku,
        int quantity,
        int cancelledQty,
        int returnedQty,
        MoneyDto unitPrice,
        MoneyDto lineTotal,
        String status
) {}