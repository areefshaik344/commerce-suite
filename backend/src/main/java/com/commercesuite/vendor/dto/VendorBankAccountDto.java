package com.commercesuite.vendor.dto;

import com.commercesuite.vendor.entity.VendorBankAccount;
import com.commercesuite.vendor.entity.VendorVerificationStatus;
import java.time.Instant;
import java.util.UUID;

public record VendorBankAccountDto(
        UUID id, UUID vendorId, String accountHolderName, String accountNumberMasked,
        String ifscCode, String bankName, String branchName,
        VendorVerificationStatus verificationStatus, Instant verifiedAt, boolean primary) {
    public static VendorBankAccountDto from(VendorBankAccount b) {
        String masked = mask(b.getAccountNumber());
        return new VendorBankAccountDto(b.getId(), b.getVendorId(), b.getAccountHolderName(), masked,
                b.getIfscCode(), b.getBankName(), b.getBranchName(),
                b.getVerificationStatus(), b.getVerifiedAt(), b.isPrimary());
    }
    private static String mask(String n) {
        if (n == null || n.length() <= 4) return "****";
        return "****" + n.substring(n.length() - 4);
    }
}