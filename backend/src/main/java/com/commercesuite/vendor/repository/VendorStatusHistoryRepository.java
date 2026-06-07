package com.commercesuite.vendor.repository;

import com.commercesuite.vendor.entity.VendorStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorStatusHistoryRepository extends JpaRepository<VendorStatusHistory, UUID> {
    List<VendorStatusHistory> findByVendorIdOrderByChangedAtDesc(UUID vendorId);
}