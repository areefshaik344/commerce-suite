package com.commercesuite.vendor.service;

import com.commercesuite.common.exception.AppException;
import com.commercesuite.vendor.entity.Vendor;
import com.commercesuite.vendor.repository.VendorRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Ensures the authenticated user owns the vendor they are touching. */
@Component
@RequiredArgsConstructor
public class VendorOwnershipGuard {
    private final VendorRepository vendorRepo;

    @Transactional(readOnly = true)
    public Vendor requireOwnedByUser(UUID userId) {
        return vendorRepo.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound("Vendor"));
    }

    @Transactional(readOnly = true)
    public void assertOwns(UUID userId, UUID vendorId) {
        Vendor v = vendorRepo.findById(vendorId).orElseThrow(() -> AppException.notFound("Vendor"));
        if (!v.getUserId().equals(userId)) throw AppException.forbidden("Not your vendor account");
    }
}