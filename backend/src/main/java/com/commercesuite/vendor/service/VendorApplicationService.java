package com.commercesuite.vendor.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.vendor.dto.*;
import com.commercesuite.vendor.entity.*;
import com.commercesuite.vendor.event.VendorEvents.VendorAppliedEvent;
import com.commercesuite.vendor.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VendorApplicationService {

    private final VendorRepository vendorRepo;
    private final VendorApplicationRepository appRepo;
    private final VendorProfileRepository profileRepo;
    private final VendorVerificationRepository verificationRepo;
    private final SlugGenerator slugGenerator;
    private final VendorStateMachine fsm;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /** Customer applies to become a vendor. Creates Vendor (PENDING_APPLICATION) + Application (SUBMITTED). */
    @Transactional
    public VendorApplicationDto apply(UUID userId, ApplyVendorRequest req) {
        if (vendorRepo.existsByUserId(userId))
            throw AppException.conflict(ErrorCode.CONFLICT, "Vendor profile already exists for this user");

        var open = appRepo.findFirstByUserIdAndStatusIn(userId,
                List.of(VendorApplicationStatus.DRAFT,
                        VendorApplicationStatus.SUBMITTED,
                        VendorApplicationStatus.UNDER_REVIEW));
        if (open.isPresent())
            throw AppException.conflict(ErrorCode.CONFLICT, "An open vendor application already exists");

        Instant now = Instant.now(clock);
        Vendor vendor = vendorRepo.save(Vendor.builder()
                .userId(userId)
                .legalName(req.legalName().trim())
                .displayName(req.displayName().trim())
                .status(VendorStatus.PENDING_APPLICATION)
                .build());

        profileRepo.save(VendorProfile.builder()
                .vendorId(vendor.getId())
                .storeName(req.displayName().trim())
                .storeSlug(slugGenerator.uniqueSlug(req.displayName()))
                .build());

        verificationRepo.save(VendorVerification.builder().vendorId(vendor.getId()).build());

        VendorApplication app = appRepo.save(VendorApplication.builder()
                .userId(userId)
                .vendorId(vendor.getId())
                .status(VendorApplicationStatus.SUBMITTED)
                .businessName(req.businessName().trim())
                .businessType(req.businessType().trim())
                .gstin(req.gstin())
                .pan(req.pan())
                .contactEmail(req.contactEmail().trim().toLowerCase())
                .contactPhone(req.contactPhone().trim())
                .registeredAddress(req.registeredAddress().trim())
                .submittedAt(now)
                .build());

        // Move vendor PENDING_APPLICATION -> UNDER_REVIEW immediately on submit.
        fsm.transition(vendor, VendorStatus.UNDER_REVIEW, userId, "Application submitted");
        app.setStatus(VendorApplicationStatus.UNDER_REVIEW);

        events.publishEvent(new VendorAppliedEvent(vendor.getId(), userId, app.getId(), now));
        return VendorApplicationDto.from(app);
    }

    @Transactional(readOnly = true)
    public List<VendorApplicationDto> listForUser(UUID userId) {
        return appRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(VendorApplicationDto::from).toList();
    }
}