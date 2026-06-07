package com.commercesuite.vendor.dto;

import com.commercesuite.vendor.entity.VendorProfile;
import java.util.UUID;

public record VendorProfileDto(
        UUID id, UUID vendorId, String storeName, String storeSlug,
        String description, String logoUrl, String bannerUrl,
        String supportEmail, String supportPhone, String websiteUrl, String returnPolicy) {
    public static VendorProfileDto from(VendorProfile p) {
        return new VendorProfileDto(p.getId(), p.getVendorId(), p.getStoreName(), p.getStoreSlug(),
                p.getDescription(), p.getLogoUrl(), p.getBannerUrl(),
                p.getSupportEmail(), p.getSupportPhone(), p.getWebsiteUrl(), p.getReturnPolicy());
    }
}