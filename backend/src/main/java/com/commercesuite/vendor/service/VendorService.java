package com.commercesuite.vendor.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.vendor.dto.*;
import com.commercesuite.vendor.entity.*;
import com.commercesuite.vendor.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Self-service vendor operations: read me, update store profile, list docs/bank/history. */
@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepo;
    private final VendorProfileRepository profileRepo;
    private final VendorVerificationRepository verificationRepo;
    private final VendorDocumentRepository documentRepo;
    private final VendorBankAccountRepository bankRepo;
    private final VendorStatusHistoryRepository historyRepo;
    private final VendorOwnershipGuard ownership;
    private final Clock clock;

    @Transactional(readOnly = true)
    public VendorDto me(UUID userId) { return VendorDto.from(ownership.requireOwnedByUser(userId)); }

    @Transactional(readOnly = true)
    public VendorProfileDto myProfile(UUID userId) {
        Vendor v = ownership.requireOwnedByUser(userId);
        return VendorProfileDto.from(profileRepo.findByVendorId(v.getId())
                .orElseThrow(() -> AppException.notFound("VendorProfile")));
    }

    @Transactional
    public VendorProfileDto updateProfile(UUID userId, UpdateVendorProfileRequest req) {
        Vendor v = ownership.requireOwnedByUser(userId);
        VendorProfile p = profileRepo.findByVendorId(v.getId())
                .orElseThrow(() -> AppException.notFound("VendorProfile"));
        p.setStoreName(req.storeName().trim());
        p.setDescription(req.description());
        p.setLogoUrl(req.logoUrl());
        p.setBannerUrl(req.bannerUrl());
        p.setSupportEmail(req.supportEmail() == null ? null : req.supportEmail().toLowerCase().trim());
        p.setSupportPhone(req.supportPhone());
        p.setWebsiteUrl(req.websiteUrl());
        p.setReturnPolicy(req.returnPolicy());
        return VendorProfileDto.from(p);
    }

    @Transactional(readOnly = true)
    public VendorVerificationDto myVerification(UUID userId) {
        Vendor v = ownership.requireOwnedByUser(userId);
        return VendorVerificationDto.from(verificationRepo.findByVendorId(v.getId())
                .orElseThrow(() -> AppException.notFound("VendorVerification")));
    }

    @Transactional
    public VendorDocumentDto uploadDocument(UUID userId, UpsertDocumentRequest req) {
        Vendor v = ownership.requireOwnedByUser(userId);
        if (v.getStatus() == VendorStatus.DEACTIVATED)
            throw AppException.conflict(ErrorCode.CONFLICT, "Vendor is deactivated");
        VendorDocument doc = VendorDocument.builder()
                .vendorId(v.getId())
                .documentType(req.documentType())
                .documentNumber(req.documentNumber())
                .fileUrl(req.fileUrl())
                .fileMime(req.fileMime())
                .fileSizeBytes(req.fileSizeBytes())
                .verificationStatus(VendorVerificationStatus.PENDING)
                .uploadedAt(Instant.now(clock))
                .build();
        return VendorDocumentDto.from(documentRepo.save(doc));
    }

    @Transactional(readOnly = true)
    public List<VendorDocumentDto> listDocuments(UUID userId) {
        Vendor v = ownership.requireOwnedByUser(userId);
        return documentRepo.findByVendorIdOrderByUploadedAtDesc(v.getId())
                .stream().map(VendorDocumentDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<VendorStatusHistory> myHistory(UUID userId) {
        Vendor v = ownership.requireOwnedByUser(userId);
        return historyRepo.findByVendorIdOrderByChangedAtDesc(v.getId());
    }

    @Transactional(readOnly = true)
    public List<VendorBankAccountDto> listBankAccounts(UUID userId) {
        Vendor v = ownership.requireOwnedByUser(userId);
        return bankRepo.findByVendorId(v.getId()).stream().map(VendorBankAccountDto::from).toList();
    }
}