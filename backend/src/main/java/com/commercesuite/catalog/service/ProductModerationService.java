package com.commercesuite.catalog.service;

import com.commercesuite.catalog.dto.ProductDto;
import com.commercesuite.catalog.entity.Product;
import com.commercesuite.catalog.entity.ProductModeration;
import com.commercesuite.catalog.entity.ProductStatus;
import com.commercesuite.catalog.event.CatalogEvents.*;
import com.commercesuite.catalog.repository.ProductModerationRepository;
import com.commercesuite.catalog.repository.ProductRepository;
import com.commercesuite.common.exception.AppException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin moderation workflow — approve / reject / suspend products. */
@Service
@RequiredArgsConstructor
public class ProductModerationService {

    private final ProductRepository productRepo;
    private final ProductModerationRepository moderationRepo;
    private final ProductStateMachine fsm;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public ProductDto approve(UUID productId, UUID adminId, String notes) {
        Product p = productRepo.findById(productId).orElseThrow(() -> AppException.notFound("Product"));
        Instant now = Instant.now(clock);
        fsm.transition(p, ProductStatus.APPROVED, adminId, notes);
        p.setApprovedAt(now); p.setApprovedBy(adminId);
        moderationRepo.save(ProductModeration.builder()
                .productId(p.getId()).reviewedBy(adminId).reviewedAt(now)
                .approvedBy(adminId).approvedAt(now).reviewNotes(notes).build());
        events.publishEvent(new ProductApprovedEvent(p.getId(), p.getVendorId(), adminId, now));
        return ProductDto.from(p);
    }

    @Transactional
    public ProductDto reject(UUID productId, UUID adminId, String reason) {
        Product p = productRepo.findById(productId).orElseThrow(() -> AppException.notFound("Product"));
        Instant now = Instant.now(clock);
        fsm.transition(p, ProductStatus.REJECTED, adminId, reason);
        p.setRejectedAt(now); p.setRejectedBy(adminId);
        moderationRepo.save(ProductModeration.builder()
                .productId(p.getId()).reviewedBy(adminId).reviewedAt(now)
                .rejectedBy(adminId).rejectedAt(now).reviewNotes(reason).build());
        events.publishEvent(new ProductRejectedEvent(p.getId(), p.getVendorId(), adminId, reason, now));
        return ProductDto.from(p);
    }

    @Transactional
    public ProductDto suspend(UUID productId, UUID adminId, String reason) {
        Product p = productRepo.findById(productId).orElseThrow(() -> AppException.notFound("Product"));
        Instant now = Instant.now(clock);
        fsm.transition(p, ProductStatus.SUSPENDED, adminId, reason);
        p.setSuspendedAt(now);
        moderationRepo.save(ProductModeration.builder()
                .productId(p.getId()).reviewedBy(adminId).reviewedAt(now)
                .reviewNotes(reason).build());
        events.publishEvent(new ProductSuspendedEvent(p.getId(), p.getVendorId(), adminId, reason, now));
        return ProductDto.from(p);
    }
}