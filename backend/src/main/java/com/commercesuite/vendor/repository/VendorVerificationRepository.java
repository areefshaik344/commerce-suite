package com.commercesuite.vendor.repository;

import com.commercesuite.vendor.entity.VendorVerification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorVerificationRepository extends JpaRepository<VendorVerification, UUID> {
    Optional<VendorVerification> findByVendorId(UUID vendorId);
}