package com.commercesuite.catalog.service;

import com.commercesuite.catalog.dto.*;
import com.commercesuite.catalog.entity.*;
import com.commercesuite.catalog.event.CatalogEvents.*;
import com.commercesuite.catalog.repository.*;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.vendor.entity.Vendor;
import com.commercesuite.vendor.entity.VendorStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Vendor-facing product service: create / update / submit / archive / fetch own. */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final ProductOwnershipGuard ownership;
    private final ProductStateMachine fsm;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public ProductDto create(ActorContext actor, CreateProductRequest r) {
        Vendor vendor = ownership.requireVendorFor(actor.userId());
        if (vendor.getStatus() != VendorStatus.APPROVED)
            throw AppException.conflict(ErrorCode.CONFLICT, "Vendor must be APPROVED to create products");
        if (!categoryRepo.existsById(r.categoryId()))
            throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Category not found");
        if (r.brandId() != null && !brandRepo.existsById(r.brandId()))
            throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Brand not found");

        String slug = ensureUniqueSlug(r.slug() == null || r.slug().isBlank() ? r.title() : r.slug());
        Product p = Product.builder()
                .vendorId(vendor.getId())
                .categoryId(r.categoryId())
                .brandId(r.brandId())
                .slug(slug)
                .title(r.title().trim())
                .shortDescription(r.shortDescription())
                .description(r.description())
                .status(ProductStatus.DRAFT)
                .build();
        productRepo.save(p);
        events.publishEvent(new ProductCreatedEvent(p.getId(), vendor.getId(), Instant.now(clock)));
        return ProductDto.from(p);
    }

    @Transactional
    public ProductDto update(ActorContext actor, UUID id, UpdateProductRequest r) {
        Product p = ownership.requireOwned(id, actor);
        if (!p.getStatus().isVendorEditable() && !ownership.isAdmin(actor))
            throw AppException.conflict(ErrorCode.CONFLICT, "Product not editable in status " + p.getStatus());
        if (r.categoryId() != null) {
            if (!categoryRepo.existsById(r.categoryId()))
                throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Category not found");
            p.setCategoryId(r.categoryId());
        }
        if (r.brandId() != null) {
            if (!brandRepo.existsById(r.brandId()))
                throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Brand not found");
            p.setBrandId(r.brandId());
        }
        if (r.title() != null && !r.title().isBlank()) p.setTitle(r.title().trim());
        if (r.shortDescription() != null) p.setShortDescription(r.shortDescription());
        if (r.description() != null)      p.setDescription(r.description());
        return ProductDto.from(p);
    }

    @Transactional
    public ProductDto submit(ActorContext actor, UUID id) {
        Product p = ownership.requireOwned(id, actor);
        Instant now = Instant.now(clock);
        fsm.transition(p, ProductStatus.PENDING_REVIEW, actor.userId(), "Submitted for review");
        p.setSubmittedAt(now);
        events.publishEvent(new ProductSubmittedEvent(p.getId(), p.getVendorId(), now));
        return ProductDto.from(p);
    }

    @Transactional
    public ProductDto archive(ActorContext actor, UUID id) {
        Product p = ownership.requireOwned(id, actor);
        Instant now = Instant.now(clock);
        fsm.transition(p, ProductStatus.ARCHIVED, actor.userId(), "Archived by owner");
        p.setArchivedAt(now);
        events.publishEvent(new ProductArchivedEvent(p.getId(), p.getVendorId(), actor.userId(), now));
        return ProductDto.from(p);
    }

    @Transactional(readOnly = true)
    public ProductDto get(ActorContext actor, UUID id) {
        Product p = productRepo.findById(id).orElseThrow(() -> AppException.notFound("Product"));
        // Vendors can only see own products via this endpoint; admins always.
        if (!ownership.isAdmin(actor)) ownership.requireOwned(id, actor);
        return ProductDto.from(p);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> mine(ActorContext actor, Pageable pageable) {
        Vendor v = ownership.requireVendorFor(actor.userId());
        return productRepo.findByVendorId(v.getId(), pageable).map(ProductDto::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> publicSearch(ProductSearchCriteria c, Pageable pageable) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.from(c), ProductSpecifications.publicOnly());
        return productRepo.findAll(spec, pageable).map(ProductDto::from);
    }

    @Transactional(readOnly = true)
    public ProductDto publicBySlug(String slug) {
        Product p = productRepo.findBySlug(slug).orElseThrow(() -> AppException.notFound("Product"));
        if (p.getStatus() != ProductStatus.APPROVED) throw AppException.notFound("Product");
        return ProductDto.from(p);
    }

    private String ensureUniqueSlug(String base) {
        String slug = CatalogSlug.slugify(base);
        if (slug.isEmpty()) throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Invalid slug");
        String candidate = slug;
        int i = 1;
        while (productRepo.existsBySlug(candidate)) {
            i++; candidate = slug + "-" + i;
            if (i > 9999) throw new IllegalStateException("Slug exhaustion");
        }
        return candidate;
    }
}