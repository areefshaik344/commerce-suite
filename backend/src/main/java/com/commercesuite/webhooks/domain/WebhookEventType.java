package com.commercesuite.webhooks.domain;

import java.util.List;

/**
 * Canonical list of business event types webhooks can subscribe to.
 * Mirrors the domain event taxonomy from {@code docs/PHASE8_IMPLEMENTATION_BLUEPRINT.md}.
 * String values match the outbox {@code event_type} column verbatim.
 */
public final class WebhookEventType {
    private WebhookEventType() {}

    // Auth
    public static final String USER_REGISTERED      = "auth.user.registered";
    public static final String EMAIL_VERIFIED       = "auth.email.verified";
    // Vendor
    public static final String VENDOR_APPLIED       = "vendor.applied";
    public static final String VENDOR_APPROVED      = "vendor.approved";
    public static final String VENDOR_REJECTED      = "vendor.rejected";
    // Catalog
    public static final String PRODUCT_CREATED      = "catalog.product.created";
    public static final String PRODUCT_APPROVED     = "catalog.product.approved";
    public static final String PRODUCT_REJECTED     = "catalog.product.rejected";
    // Inventory
    public static final String INVENTORY_RESERVED   = "inventory.reserved";
    public static final String INVENTORY_RELEASED   = "inventory.released";
    public static final String LOW_STOCK_DETECTED   = "inventory.low_stock";
    // Checkout
    public static final String CHECKOUT_STARTED     = "checkout.started";
    public static final String CHECKOUT_COMPLETED   = "checkout.completed";
    public static final String CHECKOUT_CANCELLED   = "checkout.cancelled";
    // Orders
    public static final String ORDER_CREATED        = "order.created";
    public static final String ORDER_CANCELLED      = "order.cancelled";
    public static final String ORDER_DELIVERED      = "order.delivered";
    // Shipping
    public static final String SHIPMENT_CREATED     = "shipment.created";
    public static final String SHIPMENT_DELIVERED   = "shipment.delivered";
    // Returns
    public static final String RETURN_REQUESTED     = "return.requested";
    public static final String RETURN_APPROVED      = "return.approved";
    // Refunds
    public static final String REFUND_REQUESTED     = "refund.requested";
    public static final String REFUND_COMPLETED     = "refund.completed";
    // Payments
    public static final String PAYMENT_CAPTURED     = "payment.captured";
    public static final String PAYMENT_FAILED       = "payment.failed";
    // Settlements
    public static final String SETTLEMENT_CALCULATED= "settlement.calculated";
    public static final String SETTLEMENT_PAID      = "settlement.paid";
    // Payouts
    public static final String PAYOUT_CREATED       = "payout.created";
    public static final String PAYOUT_COMPLETED     = "payout.completed";
    // Platform services
    public static final String NOTIFICATION_DELIVERED = "notification.delivered";
    public static final String AUDIT_RECORD_CREATED   = "audit.record_created";
    public static final String ANALYTICS_EVENT_RECORDED = "analytics.event_recorded";

    public static List<String> all() {
        return List.of(
                USER_REGISTERED, EMAIL_VERIFIED,
                VENDOR_APPLIED, VENDOR_APPROVED, VENDOR_REJECTED,
                PRODUCT_CREATED, PRODUCT_APPROVED, PRODUCT_REJECTED,
                INVENTORY_RESERVED, INVENTORY_RELEASED, LOW_STOCK_DETECTED,
                CHECKOUT_STARTED, CHECKOUT_COMPLETED, CHECKOUT_CANCELLED,
                ORDER_CREATED, ORDER_CANCELLED, ORDER_DELIVERED,
                SHIPMENT_CREATED, SHIPMENT_DELIVERED,
                RETURN_REQUESTED, RETURN_APPROVED,
                REFUND_REQUESTED, REFUND_COMPLETED,
                PAYMENT_CAPTURED, PAYMENT_FAILED,
                SETTLEMENT_CALCULATED, SETTLEMENT_PAID,
                PAYOUT_CREATED, PAYOUT_COMPLETED,
                NOTIFICATION_DELIVERED, AUDIT_RECORD_CREATED, ANALYTICS_EVENT_RECORDED);
    }

    public static boolean isKnown(String eventType) {
        return eventType != null && all().contains(eventType);
    }
}