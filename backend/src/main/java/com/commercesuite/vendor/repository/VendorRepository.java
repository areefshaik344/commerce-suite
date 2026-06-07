package com.commercesuite.vendor.repository;

import com.commercesuite.vendor.entity.Vendor;
import com.commercesuite.vendor.entity.VendorStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    Optional<Vendor> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    Page<Vendor> findAllByStatus(VendorStatus status, Pageable pageable);
}