package com.commercesuite.catalog.service;

import com.commercesuite.catalog.dto.CategoryDto;
import com.commercesuite.catalog.dto.UpsertCategoryRequest;
import com.commercesuite.catalog.entity.Category;
import com.commercesuite.catalog.repository.CategoryRepository;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repo;

    @Transactional
    public CategoryDto create(UpsertCategoryRequest r) {
        if (r.parentId() != null && !repo.existsById(r.parentId()))
            throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Parent category not found");
        String slug = ensureUniqueSlug(r.slug() == null || r.slug().isBlank() ? r.name() : r.slug(), null);
        Category c = Category.builder()
                .parentId(r.parentId())
                .name(r.name().trim())
                .slug(slug)
                .description(r.description())
                .icon(r.icon())
                .sortOrder(r.sortOrder() == null ? 0 : r.sortOrder())
                .active(r.active() == null ? true : r.active())
                .build();
        return CategoryDto.from(repo.save(c));
    }

    @Transactional
    public CategoryDto update(UUID id, UpsertCategoryRequest r) {
        Category c = repo.findById(id).orElseThrow(() -> AppException.notFound("Category"));
        if (r.parentId() != null) {
            if (r.parentId().equals(c.getId()))
                throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Category cannot be its own parent");
            if (!repo.existsById(r.parentId()))
                throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Parent category not found");
        }
        c.setParentId(r.parentId());
        c.setName(r.name().trim());
        if (r.slug() != null && !r.slug().isBlank() && !r.slug().equals(c.getSlug()))
            c.setSlug(ensureUniqueSlug(r.slug(), id));
        c.setDescription(r.description());
        c.setIcon(r.icon());
        if (r.sortOrder() != null) c.setSortOrder(r.sortOrder());
        if (r.active()    != null) c.setActive(r.active());
        return CategoryDto.from(c);
    }

    @Transactional
    public void delete(UUID id) {
        if (repo.existsByParentId(id))
            throw AppException.conflict(ErrorCode.CONFLICT, "Category has children");
        Category c = repo.findById(id).orElseThrow(() -> AppException.notFound("Category"));
        repo.delete(c); // soft delete via @SQLDelete
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> tree() {
        List<Category> all = repo.findAllByOrderBySortOrderAscNameAsc();
        Map<UUID, CategoryDto> byId = new LinkedHashMap<>();
        List<CategoryDto> roots = new ArrayList<>();
        for (Category c : all) byId.put(c.getId(), CategoryDto.from(c));
        for (Category c : all) {
            CategoryDto dto = byId.get(c.getId());
            if (c.getParentId() == null) roots.add(dto);
            else {
                CategoryDto parent = byId.get(c.getParentId());
                if (parent != null) parent.children().add(dto);
                else roots.add(dto);
            }
        }
        return roots;
    }

    @Transactional(readOnly = true)
    public CategoryDto getBySlug(String slug) {
        return CategoryDto.from(repo.findBySlug(slug).orElseThrow(() -> AppException.notFound("Category")));
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