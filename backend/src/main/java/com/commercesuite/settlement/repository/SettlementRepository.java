package com.commercesuite.settlement.repository;
import com.commercesuite.settlement.entity.Settlement;
import com.commercesuite.settlement.entity.SettlementStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
  Page<Settlement> findByVendorId(UUID vendorId, Pageable p);
  List<Settlement> findByStatus(SettlementStatus status);
}