package com.commercesuite.vendor.dto;

import jakarta.validation.constraints.*;

public record UpsertBankAccountRequest(
        @NotBlank @Size(max = 120) String accountHolderName,
        @NotBlank @Pattern(regexp = "^[0-9]{6,20}$", message = "Invalid account number") String accountNumber,
        @NotBlank @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC") String ifscCode,
        @NotBlank @Size(max = 120) String bankName,
        @Size(max = 120) String branchName
) {}