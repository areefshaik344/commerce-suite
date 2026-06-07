package com.commercesuite.orders.service;

import com.commercesuite.catalog.entity.Product;
import com.commercesuite.catalog.entity.ProductVariant;
import com.commercesuite.catalog.repository.ProductRepository;
import com.commercesuite.catalog.repository.ProductVariantRepository;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.user.entity.Address;
import com.commercesuite.user.repository.AddressRepository;
import com.commercesuite.vendor.entity.Vendor;
import com.commercesuite.vendor.repository.VendorRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Captures immutable snapshots of product/variant/vendor/address at order placement.
 * Stored as JSON strings — historical orders are NEVER recomputed from live data.
 */
@Service
@RequiredArgsConstructor
public class OrderSnapshotService {

    private final ProductRepository productRepo;
    private final ProductVariantRepository variantRepo;
    private final VendorRepository vendorRepo;
    private final AddressRepository addressRepo;

    public String productSnapshot(UUID productId, UUID variantId) {
        Product p = productRepo.findById(productId).orElseThrow(() -> AppException.notFound("Product"));
        ProductVariant v = variantRepo.findById(variantId).orElseThrow(() -> AppException.notFound("Variant"));
        return "{\"productId\":\"" + p.getId() + "\"," +
               "\"variantId\":\"" + v.getId() + "\"," +
               "\"name\":" + json(p.getName()) + "," +
               "\"sku\":" + json(v.getSku()) + "}";
    }

    public String vendorSnapshot(UUID vendorId) {
        Vendor v = vendorRepo.findById(vendorId).orElseThrow(() -> AppException.notFound("Vendor"));
        return "{\"vendorId\":\"" + v.getId() + "\"," +
               "\"displayName\":" + json(v.getDisplayName()) + "," +
               "\"legalName\":" + json(v.getLegalName()) + "}";
    }

    public String addressSnapshot(UUID addressId) {
        if (addressId == null) return "{}";
        Address a = addressRepo.findById(addressId).orElseThrow(() -> AppException.notFound("Address"));
        return "{\"addressId\":\"" + a.getId() + "\"," +
               "\"line1\":" + json(a.getLine1()) + "," +
               "\"line2\":" + json(a.getLine2()) + "," +
               "\"city\":" + json(a.getCity()) + "," +
               "\"state\":" + json(a.getState()) + "," +
               "\"postalCode\":" + json(a.getPostalCode()) + "," +
               "\"country\":" + json(a.getCountry()) + "}";
    }

    private String json(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
