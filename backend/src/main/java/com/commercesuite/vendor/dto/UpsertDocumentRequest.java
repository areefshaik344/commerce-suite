package com.commercesuite.vendor.dto;

import com.commercesuite.vendor.entity.VendorDocumentType;
import jakarta.validation.constraints.*;

public record UpsertDocumentRequest(
        @NotNull VendorDocumentType documentType,
        @Size(max = 80)  String documentNumber,
        @Size(max = 500) String fileUrl,
        @Size(max = 80)  String fileMime,
        @PositiveOrZero  Long fileSizeBytes
) {}