package com.commercesuite.catalog.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** FSM for product lifecycle. See docs/CATALOG_MODULE.md. */
public enum ProductStatus {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    ARCHIVED,
    SUSPENDED;

    private static final Map<ProductStatus, Set<ProductStatus>> ALLOWED = Map.of(
            DRAFT,           EnumSet.of(PENDING_REVIEW, ARCHIVED),
            PENDING_REVIEW,  EnumSet.of(APPROVED, REJECTED, DRAFT, ARCHIVED),
            APPROVED,        EnumSet.of(PENDING_REVIEW, SUSPENDED, ARCHIVED),
            REJECTED,        EnumSet.of(DRAFT, ARCHIVED),
            SUSPENDED,       EnumSet.of(APPROVED, ARCHIVED),
            ARCHIVED,        EnumSet.noneOf(ProductStatus.class)
    );

    public boolean canTransitionTo(ProductStatus next) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(next);
    }
    public boolean isTerminal()    { return this == ARCHIVED; }
    public boolean isPubliclyVisible() { return this == APPROVED; }
    public boolean isVendorEditable()  { return this == DRAFT || this == REJECTED; }
}