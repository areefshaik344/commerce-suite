package com.commercesuite.cart.service;

import com.commercesuite.catalog.entity.Product;
import com.commercesuite.catalog.entity.ProductStatus;
import com.commercesuite.catalog.entity.ProductVariant;
import com.commercesuite.catalog.repository.ProductRepository;
import com.commercesuite.catalog.repository.ProductVariantRepository;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.inventory.entity.InventoryItem;
import com.commercesuite.inventory.repository.InventoryItemRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hard validation for every cart mutation.
 * - Variant + product must exist and be APPROVED + active.
 * - Product must not be archived, suspended, deleted, or rejected.
 * - qty must be positive.
 * - qty <= available inventory.
 */
@Service
@RequiredArgsConstructor
public class CartValidationService {

    private final ProductRepository productRepo;
    private final ProductVariantRepository variantRepo;
    private final InventoryItemRepository inventoryRepo;

    @Transactional(readOnly = true)
    public ValidatedLine validate(UUID variantId, int qty) {
        if (qty <= 0)
            throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Quantity must be positive");

        ProductVariant variant = variantRepo.findById(variantId)
                .orElseThrow(() -> AppException.notFound("Variant"));
        if (!variant.isActive())
            throw AppException.conflict(ErrorCode.CONFLICT, "Variant is inactive");

        Product product = productRepo.findById(variant.getProductId())
                .orElseThrow(() -> AppException.notFound("Product"));
        if (product.getStatus() != ProductStatus.APPROVED)
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Product not available for purchase: " + product.getStatus());
        if (product.getArchivedAt() != null || product.getSuspendedAt() != null)
            throw AppException.conflict(ErrorCode.CONFLICT, "Product unavailable");

        InventoryItem inv = inventoryRepo.findByVariantId(variantId)
                .orElseThrow(() -> AppException.conflict(ErrorCode.CONFLICT,
                        "Inventory not initialised for variant"));
        int available = inv.getOnHandQty() - inv.getReservedQty();
        if (qty > available)
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Insufficient stock: requested=" + qty + " available=" + available);

        return new ValidatedLine(product, variant, available);
    }

    public record ValidatedLine(Product product, ProductVariant variant, int available) {}
}