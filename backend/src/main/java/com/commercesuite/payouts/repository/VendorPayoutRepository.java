package com.commercesuite.payouts.repository;
import com.commercesuite.payouts.entity.PayoutStatus;
import com.commercesuite.payouts.entity.VendorPayout;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorPayoutRepository extends JpaRepository<VendorPayout, UUID> {
  Page<VendorPayout> findByVendorId(UUID vendorId, Pageable p);
  List<VendorPayout> findByBatchId(UUID batchId);
  List<VendorPayout> findByStatus(PayoutStatus status);
}