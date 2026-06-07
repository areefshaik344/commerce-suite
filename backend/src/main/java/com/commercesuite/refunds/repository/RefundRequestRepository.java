package com.commercesuite.refunds.repository;
import com.commercesuite.refunds.entity.RefundRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {
  Page<RefundRequest> findAll(Pageable pageable);
  List<RefundRequest> findByOrderId(UUID orderId);
  List<RefundRequest> findByVendorOrderId(UUID vendorOrderId);
}
