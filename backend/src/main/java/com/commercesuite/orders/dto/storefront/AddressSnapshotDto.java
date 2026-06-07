package com.commercesuite.orders.dto.storefront;

/**
 * Address as stored on the order snapshot. Raw JSON forwarded to the
 * frontend; the same shape used in checkout snapshots.
 */
public record AddressSnapshotDto(String json) {}