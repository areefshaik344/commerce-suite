package com.commercesuite.catalog.event;

import java.time.Instant;
import java.util.UUID;

/** Catalog domain events. Stable payloads — see docs/CATALOG_MODULE.md. */
public final class CatalogEvents {
    private CatalogEvents() {}
    public record ProductCreatedEvent     (UUID productId, UUID vendorId, Instant at) {}
    public record ProductSubmittedEvent   (UUID productId, UUID vendorId, Instant at) {}
    public record ProductApprovedEvent    (UUID productId, UUID vendorId, UUID approvedBy, Instant at) {}
    public record ProductRejectedEvent    (UUID productId, UUID vendorId, UUID rejectedBy, String reason, Instant at) {}
    public record ProductSuspendedEvent   (UUID productId, UUID vendorId, UUID by, String reason, Instant at) {}
    public record ProductArchivedEvent    (UUID productId, UUID vendorId, UUID by, Instant at) {}
    public record ProductReviewCreatedEvent(UUID reviewId, UUID productId, UUID customerId, int rating, Instant at) {}
}