package com.commercesuite.inventory.entity;

public enum ReservationReleaseReason {
    ABANDONED, PAYMENT_FAILED, PAYMENT_CANCELLED,
    TTL_EXPIRED, EXPLICIT_RELEASE, USER_LOGOUT, COMMITTED
}