package com.commercesuite.user.dto;

import com.commercesuite.user.entity.AddressType;
import jakarta.validation.constraints.*;

public record UpsertAddressRequest(
    @NotNull AddressType type,
    @NotBlank @Size(max = 80)  String contactName,
    @NotBlank @Pattern(regexp = "^\\+?[0-9 \\-]{10,15}$") String phone,
    @NotBlank @Size(max = 120) String line1,
    @Size(max = 120)           String line2,
    @NotBlank @Size(max = 60)  String city,
    @NotBlank @Size(max = 60)  String state,
    @NotBlank @Pattern(regexp = "^\\d{6}$") String pincode,
    @Pattern(regexp = "^[A-Z]{2}$") String country,
    boolean isDefault
) {}
