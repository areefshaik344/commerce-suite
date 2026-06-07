package com.commercesuite.user.dto;

import com.commercesuite.user.entity.AddressType;
import java.util.UUID;

public record AddressDto(
    UUID id, AddressType type, String contactName, String phone,
    String line1, String line2, String city, String state, String pincode,
    String country, boolean isDefault
) {}
