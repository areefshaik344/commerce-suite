package com.commercesuite.vendor.service;

import com.commercesuite.vendor.dto.*;
import com.commercesuite.vendor.entity.*;
import com.commercesuite.vendor.repository.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VendorBankService {
    private final VendorBankAccountRepository bankRepo;
    private final VendorOwnershipGuard ownership;

    @Transactional
    public VendorBankAccountDto upsertPrimary(UUID userId, UpsertBankAccountRequest req) {
        Vendor v = ownership.requireOwnedByUser(userId);
        VendorBankAccount account = bankRepo.findFirstByVendorIdAndPrimaryTrue(v.getId())
                .orElseGet(() -> VendorBankAccount.builder().vendorId(v.getId()).primary(true).build());

        account.setAccountHolderName(req.accountHolderName().trim());
        account.setAccountNumber(req.accountNumber());
        account.setIfscCode(req.ifscCode().toUpperCase());
        account.setBankName(req.bankName().trim());
        account.setBranchName(req.branchName());
        // Any change resets verification — penny-drop must be re-issued.
        account.setVerificationStatus(VendorVerificationStatus.PENDING);
        account.setVerifiedAt(null);
        account.setPennyDropRef(null);
        return VendorBankAccountDto.from(bankRepo.save(account));
    }
}