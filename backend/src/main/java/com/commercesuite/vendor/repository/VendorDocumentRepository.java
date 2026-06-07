package com.commercesuite.vendor.repository;

import com.commercesuite.vendor.entity.VendorDocument;
import com.commercesuite.vendor.entity.VendorDocumentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorDocumentRepository extends JpaRepository<VendorDocument, UUID> {
    List<VendorDocument> findByVendorIdOrderByUploadedAtDesc(UUID vendorId);
    Optional<VendorDocument> findFirstByVendorIdAndDocumentType(UUID vendorId, VendorDocumentType type);
}