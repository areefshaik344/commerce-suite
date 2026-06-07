package com.commercesuite.vendor.dto;

import jakarta.validation.constraints.*;

public record ApplyVendorRequest(
        @NotBlank @Size(max = 160) String legalName,
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Size(max = 160) String businessName,
        @NotBlank @Size(max = 60)  String businessType,
        @Pattern(regexp = "^[0-9A-Z]{15}$", message = "Invalid GSTIN") String gstin,
        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN") String pan,
        @NotBlank @Email @Size(max = 255) String contactEmail,
        @NotBlank @Pattern(regexp = "^[+0-9 \\-]{7,20}$") String contactPhone,
        @NotBlank @Size(max = 2000) String registeredAddress
) {}