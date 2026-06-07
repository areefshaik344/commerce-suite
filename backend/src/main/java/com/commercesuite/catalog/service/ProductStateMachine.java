package com.commercesuite.catalog.service;

import com.commercesuite.catalog.entity.Product;
import com.commercesuite.catalog.entity.ProductStatus;
import com.commercesuite.catalog.entity.ProductStatusHistory;
import com.commercesuite.catalog.repository.ProductStatusHistoryRepository;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Enforces Product FSM and records audit history. */
@Component
@RequiredArgsConstructor
public class ProductStateMachine {
    private final ProductStatusHistoryRepository historyRepo;
    private final Clock clock;

    public void transition(Product product, ProductStatus next, UUID actorId, String reason) {
        ProductStatus prev = product.getStatus();
        if (!prev.canTransitionTo(next))
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Illegal product transition " + prev + " -> " + next);
        product.setStatus(next);
        product.setStatusReason(reason);
        historyRepo.save(ProductStatusHistory.builder()
                .productId(product.getId())
                .fromStatus(prev).toStatus(next)
                .reason(reason).changedBy(actorId)
                .changedAt(Instant.now(clock))
                .build());
    }
}