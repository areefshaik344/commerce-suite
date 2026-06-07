package com.commercesuite.vendor.entity;

public enum VendorApplicationStatus {
    DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, WITHDRAWN;

    public boolean isOpen()      { return this == DRAFT || this == SUBMITTED || this == UNDER_REVIEW; }
    public boolean isTerminal()  { return this == APPROVED || this == REJECTED || this == WITHDRAWN; }
}