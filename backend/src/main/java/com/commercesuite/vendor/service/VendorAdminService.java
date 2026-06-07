package com.commercesuite.vendor.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.user.entity.AccountStatus;
import com.commercesuite.user.entity.User;
import com.commercesuite.user.repository.UserRepository;
import com.commercesuite.vendor.dto.*;
import com.commercesuite.vendor.entity.*;
import com.commercesuite.vendor.event.VendorEvents.*;
import com.commercesuite.vendor.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorAdminService {

    private final VendorRepository vendorRepo;
    private final VendorApplicationRepository appRepo;
    private final UserRepository userRepo;
    private final RoleService roleService;
    private final VendorStateMachine fsm;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<VendorDto> list(VendorStatus status, Pageable pageable) {
        Page<Vendor> page = (status == null)
                ? vendorRepo.findAll(pageable)
                : vendorRepo.findAllByStatus(status, pageable);
        return page.map(VendorDto::from);
    }

    @Transactional(readOnly = true)
    public VendorDto get(UUID vendorId) {
        return VendorDto.from(vendorRepo.findById(vendorId)
                .orElseThrow(() -> AppException.notFound("Vendor")));
    }

    @Transactional
    public VendorDto approve(UUID vendorId, UUID adminId, String reason) {
        Vendor v = vendorRepo.findById(vendorId).orElseThrow(() -> AppException.notFound("Vendor"));
        Instant now = Instant.now(clock);
        fsm.transition(v, VendorStatus.APPROVED, adminId, reason);
        v.setApprovedAt(now);
        v.setApprovedBy(adminId);

        // Close the open application
        closeOpenApplication(v.getUserId(), VendorApplicationStatus.APPROVED, adminId, reason);

        // Grant VENDOR role + reactivate user account if pending-vendor.
        roleService.grant(v.getUserId(), AppRole.VENDOR, adminId);
        userRepo.findById(v.getUserId()).ifPresent(u -> {
            if (u.getAccountStatus() == AccountStatus.PENDING_VENDOR_APPROVAL)
                u.setAccountStatus(AccountStatus.ACTIVE);
        });

        events.publishEvent(new VendorApprovedEvent(v.getId(), adminId, now));
        return VendorDto.from(v);
    }

    @Transactional
    public VendorDto reject(UUID vendorId, UUID adminId, String reason) {
        Vendor v = vendorRepo.findById(vendorId).orElseThrow(() -> AppException.notFound("Vendor"));
        Instant now = Instant.now(clock);
        fsm.transition(v, VendorStatus.REJECTED, adminId, reason);
        v.setRejectedAt(now);
        var app = closeOpenApplication(v.getUserId(), VendorApplicationStatus.REJECTED, adminId, reason);
        events.publishEvent(new VendorRejectedEvent(v.getId(),
                app == null ? null : app.getId(), adminId, reason, now));
        return VendorDto.from(v);
    }

    @Transactional
    public VendorDto suspend(UUID vendorId, UUID adminId, String reason) {
        Vendor v = vendorRepo.findById(vendorId).orElseThrow(() -> AppException.notFound("Vendor"));
        if (v.getStatus() != VendorStatus.APPROVED)
            throw AppException.conflict(ErrorCode.CONFLICT, "Only APPROVED vendors can be suspended");
        Instant now = Instant.now(clock);
        fsm.transition(v, VendorStatus.SUSPENDED, adminId, reason);
        v.setSuspendedAt(now);
        events.publishEvent(new VendorSuspendedEvent(v.getId(), adminId, reason, now));
        return VendorDto.from(v);
    }

    @Transactional
    public VendorDto reactivate(UUID vendorId, UUID adminId, String reason) {
        Vendor v = vendorRepo.findById(vendorId).orElseThrow(() -> AppException.notFound("Vendor"));
        if (v.getStatus() != VendorStatus.SUSPENDED)
            throw AppException.conflict(ErrorCode.CONFLICT, "Only SUSPENDED vendors can be reactivated");
        Instant now = Instant.now(clock);
        fsm.transition(v, VendorStatus.APPROVED, adminId, reason);
        v.setSuspendedAt(null);
        v.setApprovedAt(now);
        v.setApprovedBy(adminId);
        events.publishEvent(new VendorReactivatedEvent(v.getId(), adminId, now));
        return VendorDto.from(v);
    }

    @Transactional
    public VendorDto deactivate(UUID vendorId, UUID adminId, String reason) {
        Vendor v = vendorRepo.findById(vendorId).orElseThrow(() -> AppException.notFound("Vendor"));
        Instant now = Instant.now(clock);
        fsm.transition(v, VendorStatus.DEACTIVATED, adminId, reason);
        v.setDeactivatedAt(now);
        roleService.revoke(v.getUserId(), AppRole.VENDOR);
        events.publishEvent(new VendorDeactivatedEvent(v.getId(), adminId, reason, now));
        return VendorDto.from(v);
    }

    private VendorApplication closeOpenApplication(UUID userId,
                                                    VendorApplicationStatus terminal,
                                                    UUID adminId, String reason) {
        var open = appRepo.findFirstByUserIdAndStatusIn(userId,
                List.of(VendorApplicationStatus.DRAFT,
                        VendorApplicationStatus.SUBMITTED,
                        VendorApplicationStatus.UNDER_REVIEW));
        if (open.isEmpty()) return null;
        var app = open.get();
        app.setStatus(terminal);
        app.setReviewedAt(Instant.now(clock));
        app.setReviewedBy(adminId);
        app.setReviewNotes(reason);
        return app;
    }
}