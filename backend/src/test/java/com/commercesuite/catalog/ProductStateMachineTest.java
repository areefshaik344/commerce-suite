package com.commercesuite.catalog;

import com.commercesuite.catalog.entity.ProductStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductStateMachineTest {
    @Test void legal() {
        assertTrue(ProductStatus.DRAFT.canTransitionTo(ProductStatus.PENDING_REVIEW));
        assertTrue(ProductStatus.PENDING_REVIEW.canTransitionTo(ProductStatus.APPROVED));
        assertTrue(ProductStatus.PENDING_REVIEW.canTransitionTo(ProductStatus.REJECTED));
        assertTrue(ProductStatus.APPROVED.canTransitionTo(ProductStatus.SUSPENDED));
        assertTrue(ProductStatus.SUSPENDED.canTransitionTo(ProductStatus.APPROVED));
        assertTrue(ProductStatus.REJECTED.canTransitionTo(ProductStatus.DRAFT));
    }
    @Test void illegal() {
        assertFalse(ProductStatus.DRAFT.canTransitionTo(ProductStatus.APPROVED));
        assertFalse(ProductStatus.ARCHIVED.canTransitionTo(ProductStatus.APPROVED));
        assertFalse(ProductStatus.APPROVED.canTransitionTo(ProductStatus.DRAFT));
    }
}