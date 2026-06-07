package com.commercesuite.vendor.dto;

import com.commercesuite.vendor.entity.VendorVerification;
import com.commercesuite.vendor.entity.VendorVerificationStatus;
import java.time.Instant;
import java.util.UUID;

public record VendorVerificationDto(
        UUID vendorId,
        VendorVerificationStatus gstStatus, Instant gstVerifiedAt,
        VendorVerificationStatus panStatus, Instant panVerifiedAt,
        VendorVerificationStatus bankStatus, Instant bankVerifiedAt,
        VendorVerificationStatus businessStatus, Instant businessVerifiedAt,
        boolean fullyVerified) {
    public static VendorVerificationDto from(VendorVerification v) {
        return new VendorVerificationDto(v.getVendorId(),
                v.getGstStatus(), v.getGstVerifiedAt(),
                v.getPanStatus(), v.getPanVerifiedAt(),
                v.getBankStatus(), v.getBankVerifiedAt(),
                v.getBusinessStatus(), v.getBusinessVerifiedAt(),
                v.isFullyVerified());
    }
}