package com.commercesuite.commission.repository;
import com.commercesuite.commission.entity.CommissionCalculation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionCalculationRepository extends JpaRepository<CommissionCalculation, UUID> {
  Optional<CommissionCalculation> findByVendorOrderId(UUID vendorOrderId);
  List<CommissionCalculation> findByVendorId(UUID vendorId);
}