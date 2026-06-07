package com.commercesuite.orders.dto.storefront;

import java.util.List;
import java.util.UUID;

public record OrderTimelineDto(UUID orderId, List<OrderTimelineEntryDto> entries) {}