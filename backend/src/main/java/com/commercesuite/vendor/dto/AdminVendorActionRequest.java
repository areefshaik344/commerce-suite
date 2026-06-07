package com.commercesuite.vendor.dto;

import jakarta.validation.constraints.Size;

public record AdminVendorActionRequest(@Size(max = 1000) String reason) {}