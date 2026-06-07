package com.commercesuite.vendor.repository;

import com.commercesuite.vendor.entity.VendorApplication;
import com.commercesuite.vendor.entity.VendorApplicationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorApplicationRepository extends JpaRepository<VendorApplication, UUID> {
    List<VendorApplication> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<VendorApplication> findFirstByUserIdAndStatusIn(UUID userId, List<VendorApplicationStatus> statuses);
}