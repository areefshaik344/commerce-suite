package com.commercesuite.returns.repository;
import com.commercesuite.returns.entity.ReturnRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {
  Page<ReturnRequest> findByCustomerId(UUID customerId, Pageable pageable);
  Page<ReturnRequest> findByVendorId(UUID vendorId, Pageable pageable);
  List<ReturnRequest> findByVendorOrderId(UUID vendorOrderId);
}
