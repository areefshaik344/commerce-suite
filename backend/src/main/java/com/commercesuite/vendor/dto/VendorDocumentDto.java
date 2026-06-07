package com.commercesuite.vendor.dto;

import com.commercesuite.vendor.entity.VendorDocument;
import com.commercesuite.vendor.entity.VendorDocumentType;
import com.commercesuite.vendor.entity.VendorVerificationStatus;
import java.time.Instant;
import java.util.UUID;

public record VendorDocumentDto(
        UUID id, UUID vendorId, VendorDocumentType documentType, String documentNumber,
        String fileUrl, String fileMime, Long fileSizeBytes,
        VendorVerificationStatus verificationStatus, String reviewNotes,
        Instant reviewedAt, Instant uploadedAt) {
    public static VendorDocumentDto from(VendorDocument d) {
        return new VendorDocumentDto(d.getId(), d.getVendorId(), d.getDocumentType(), d.getDocumentNumber(),
                d.getFileUrl(), d.getFileMime(), d.getFileSizeBytes(),
                d.getVerificationStatus(), d.getReviewNotes(), d.getReviewedAt(), d.getUploadedAt());
    }
}