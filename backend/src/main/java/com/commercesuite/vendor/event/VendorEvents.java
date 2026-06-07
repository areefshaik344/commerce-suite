package com.commercesuite.vendor.event;

import java.time.Instant;
import java.util.UUID;

/** Domain events published via Spring's ApplicationEventPublisher. Stable payloads. */
public final class VendorEvents {
    private VendorEvents() {}
    public record VendorAppliedEvent       (UUID vendorId, UUID userId, UUID applicationId, Instant at) {}
    public record VendorApprovedEvent      (UUID vendorId, UUID approvedBy, Instant at) {}
    public record VendorRejectedEvent      (UUID vendorId, UUID applicationId, UUID rejectedBy, String reason, Instant at) {}
    public record VendorSuspendedEvent     (UUID vendorId, UUID by, String reason, Instant at) {}
    public record VendorReactivatedEvent   (UUID vendorId, UUID by, Instant at) {}
    public record VendorDeactivatedEvent   (UUID vendorId, UUID by, String reason, Instant at) {}
}