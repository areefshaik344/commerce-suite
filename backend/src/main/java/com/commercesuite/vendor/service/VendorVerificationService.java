package com.commercesuite.vendor.service;

import com.commercesuite.common.exception.AppException;
import com.commercesuite.vendor.entity.VendorVerification;
import com.commercesuite.vendor.entity.VendorVerificationStatus;
import com.commercesuite.vendor.repository.VendorVerificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin/system updates to per-vendor verification flags. */
@Service
@RequiredArgsConstructor
public class VendorVerificationService {
    private final VendorVerificationRepository repo;
    private final Clock clock;

    @Transactional
    public VendorVerification mark(UUID vendorId, String kind, VendorVerificationStatus s) {
        VendorVerification v = repo.findByVendorId(vendorId)
                .orElseThrow(() -> AppException.notFound("VendorVerification"));
        Instant now = Instant.now(clock);
        switch (kind) {
            case "GST"      -> { v.setGstStatus(s);      v.setGstVerifiedAt(verifiedAt(s, now)); }
            case "PAN"      -> { v.setPanStatus(s);      v.setPanVerifiedAt(verifiedAt(s, now)); }
            case "BANK"     -> { v.setBankStatus(s);     v.setBankVerifiedAt(verifiedAt(s, now)); }
            case "BUSINESS" -> { v.setBusinessStatus(s); v.setBusinessVerifiedAt(verifiedAt(s, now)); }
            default -> throw AppException.badRequest(com.commercesuite.common.api.ErrorCode.VALIDATION_FAILED,
                    "Unknown verification kind: " + kind);
        }
        return v;
    }
    private Instant verifiedAt(VendorVerificationStatus s, Instant now) {
        return s == VendorVerificationStatus.VERIFIED ? now : null;
    }
}