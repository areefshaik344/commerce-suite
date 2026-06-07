package com.commercesuite.catalog.service;

import com.commercesuite.catalog.dto.ProductVariantDto;
import com.commercesuite.catalog.dto.UpsertVariantRequest;
import com.commercesuite.catalog.entity.Product;
import com.commercesuite.catalog.entity.ProductVariant;
import com.commercesuite.catalog.repository.ProductVariantRepository;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository variantRepo;
    private final ProductOwnershipGuard ownership;

    @Transactional
    public ProductVariantDto create(ActorContext actor, UUID productId, UpsertVariantRequest r) {
        Product p = ownership.requireOwned(productId, actor);
        if (variantRepo.existsBySku(r.sku()))
            throw AppException.conflict(ErrorCode.CONFLICT, "SKU already exists");
        ProductVariant v = ProductVariant.builder()
                .productId(p.getId())
                .sku(r.sku().trim()).barcode(r.barcode())
                .pricePaise(r.pricePaise()).compareAtPaise(r.compareAtPaise())
                .currency("INR")
                .weightGrams(r.weightGrams()).lengthMm(r.lengthMm())
                .widthMm(r.widthMm()).heightMm(r.heightMm())
                .optionsJson(r.optionsJson() == null ? "{}" : r.optionsJson())
                .isDefault(Boolean.TRUE.equals(r.isDefault()))
                .active(r.active() == null ? true : r.active())
                .build();
        return ProductVariantDto.from(variantRepo.save(v));
    }

    @Transactional(readOnly = true)
    public List<ProductVariantDto> list(ActorContext actor, UUID productId) {
        ownership.requireOwned(productId, actor);
        return variantRepo.findByProductIdOrderByCreatedAtAsc(productId)
                .stream().map(ProductVariantDto::from).toList();
    }
}