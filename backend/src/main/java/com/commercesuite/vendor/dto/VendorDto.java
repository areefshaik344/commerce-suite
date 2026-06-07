package com.commercesuite.vendor.dto;

import com.commercesuite.vendor.entity.Vendor;
import com.commercesuite.vendor.entity.VendorStatus;
import java.time.Instant;
import java.util.UUID;

public record VendorDto(
        UUID id, UUID userId, String legalName, String displayName,
        VendorStatus status, String statusReason,
        Instant approvedAt, Instant rejectedAt, Instant suspendedAt, Instant deactivatedAt,
        Instant createdAt, Instant updatedAt) {
    public static VendorDto from(Vendor v) {
        return new VendorDto(v.getId(), v.getUserId(), v.getLegalName(), v.getDisplayName(),
                v.getStatus(), v.getStatusReason(),
                v.getApprovedAt(), v.getRejectedAt(), v.getSuspendedAt(), v.getDeactivatedAt(),
                v.getCreatedAt(), v.getUpdatedAt());
    }
}