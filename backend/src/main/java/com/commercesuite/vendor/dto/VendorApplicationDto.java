package com.commercesuite.vendor.dto;

import com.commercesuite.vendor.entity.VendorApplication;
import com.commercesuite.vendor.entity.VendorApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record VendorApplicationDto(
        UUID id, UUID userId, UUID vendorId, VendorApplicationStatus status,
        String businessName, String businessType, String gstin, String pan,
        String contactEmail, String contactPhone, String registeredAddress,
        Instant submittedAt, Instant reviewedAt, String reviewNotes,
        Instant createdAt) {
    public static VendorApplicationDto from(VendorApplication a) {
        return new VendorApplicationDto(a.getId(), a.getUserId(), a.getVendorId(), a.getStatus(),
                a.getBusinessName(), a.getBusinessType(), a.getGstin(), a.getPan(),
                a.getContactEmail(), a.getContactPhone(), a.getRegisteredAddress(),
                a.getSubmittedAt(), a.getReviewedAt(), a.getReviewNotes(), a.getCreatedAt());
    }
}