package com.commercesuite.catalog.service;

import com.commercesuite.catalog.dto.BrandDto;
import com.commercesuite.catalog.dto.UpsertBrandRequest;
import com.commercesuite.catalog.entity.Brand;
import com.commercesuite.catalog.repository.BrandRepository;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepository repo;

    @Transactional
    public BrandDto create(UpsertBrandRequest r) {
        String slug = ensureUniqueSlug(r.slug() == null || r.slug().isBlank() ? r.name() : r.slug(), null);
        Brand b = Brand.builder()
                .name(r.name().trim()).slug(slug)
                .description(r.description()).logoUrl(r.logoUrl())
                .active(r.active() == null ? true : r.active())
                .build();
        return BrandDto.from(repo.save(b));
    }

    @Transactional
    public BrandDto update(UUID id, UpsertBrandRequest r) {
        Brand b = repo.findById(id).orElseThrow(() -> AppException.notFound("Brand"));
        b.setName(r.name().trim());
        if (r.slug() != null && !r.slug().isBlank() && !r.slug().equals(b.getSlug()))
            b.setSlug(ensureUniqueSlug(r.slug(), id));
        b.setDescription(r.description());
        b.setLogoUrl(r.logoUrl());
        if (r.active() != null) b.setActive(r.active());
        return BrandDto.from(b);
    }

    @Transactional(readOnly = true)
    public List<BrandDto> listActive() {
        return repo.findAllByActiveTrueOrderByNameAsc().stream().map(BrandDto::from).toList();
    }

    @Transactional(readOnly = true)
    public BrandDto get(UUID id) {
        return BrandDto.from(repo.findById(id).orElseThrow(() -> AppException.notFound("Brand")));
    }

    @Transactional
    public void delete(UUID id) {
        repo.delete(repo.findById(id).orElseThrow(() -> AppException.notFound("Brand")));
    }

    private String ensureUniqueSlug(String base, UUID excludeId) {
        String slug = CatalogSlug.slugify(base);
        if (slug.isEmpty()) throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Invalid slug");
        String candidate = slug;
        int i = 1;
        while (true) {
            var existing = repo.findBySlug(candidate);
            if (existing.isEmpty() || (excludeId != null && existing.get().getId().equals(excludeId))) return candidate;
            i++; candidate = slug + "-" + i;
            if (i > 9999) throw new IllegalStateException("Slug exhaustion");
        }
    }
}