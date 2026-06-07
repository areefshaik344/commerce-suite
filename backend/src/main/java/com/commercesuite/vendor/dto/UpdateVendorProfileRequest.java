package com.commercesuite.vendor.dto;

import jakarta.validation.constraints.*;

public record UpdateVendorProfileRequest(
        @NotBlank @Size(max = 120) String storeName,
        @Size(max = 4000) String description,
        @Size(max = 500) String logoUrl,
        @Size(max = 500) String bannerUrl,
        @Email @Size(max = 255) String supportEmail,
        @Pattern(regexp = "^[+0-9 \\-]{7,20}$") String supportPhone,
        @Size(max = 500) String websiteUrl,
        @Size(max = 4000) String returnPolicy
) {}