package com.commercesuite.vendor.repository;

import com.commercesuite.vendor.entity.VendorBankAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorBankAccountRepository extends JpaRepository<VendorBankAccount, UUID> {
    List<VendorBankAccount> findByVendorId(UUID vendorId);
    Optional<VendorBankAccount> findFirstByVendorIdAndPrimaryTrue(UUID vendorId);
}