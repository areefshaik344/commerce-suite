package com.commercesuite.vendor.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.vendor.entity.Vendor;
import com.commercesuite.vendor.entity.VendorStatus;
import com.commercesuite.vendor.entity.VendorStatusHistory;
import com.commercesuite.vendor.repository.VendorStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Enforces VendorStatus FSM and records audit history. */
@Component
@RequiredArgsConstructor
public class VendorStateMachine {
    private final VendorStatusHistoryRepository historyRepo;
    private final Clock clock;

    public void transition(Vendor vendor, VendorStatus next, UUID actorId, String reason) {
        VendorStatus prev = vendor.getStatus();
        if (prev == next) return;
        if (!prev.canTransitionTo(next))
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Illegal vendor transition " + prev + " -> " + next);
        vendor.setStatus(next);
        vendor.setStatusReason(reason);
        historyRepo.save(VendorStatusHistory.builder()
                .vendorId(vendor.getId())
                .fromStatus(prev).toStatus(next)
                .reason(reason).changedBy(actorId)
                .changedAt(Instant.now(clock))
                .build());
    }
}