package com.commercesuite.vendor.repository;

import com.commercesuite.vendor.entity.VendorProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorProfileRepository extends JpaRepository<VendorProfile, UUID> {
    Optional<VendorProfile> findByVendorId(UUID vendorId);
    Optional<VendorProfile> findByStoreSlug(String slug);
    boolean existsByStoreSlug(String slug);
}