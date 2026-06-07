package com.commercesuite.analytics.service;

import com.commercesuite.analytics.domain.AnalyticsCategory;
import java.util.Map;

/**
 * Single source of truth for mapping outbox event types to analytics
 * categories + KPI metric codes. Pure / deterministic.
 */
public final class AnalyticsEventClassifier {
    private AnalyticsEventClassifier() {}

    private static final Map<String, AnalyticsCategory> CATEGORY = Map.ofEntries(
            Map.entry("auth.user_registered",       AnalyticsCategory.CUSTOMER),
            Map.entry("auth.user_logged_in",        AnalyticsCategory.CUSTOMER),
            Map.entry("vendor.applied",             AnalyticsCategory.VENDOR),
            Map.entry("vendor.approved",            AnalyticsCategory.VENDOR),
            Map.entry("product.created",            AnalyticsCategory.CATALOG),
            Map.entry("product.approved",           AnalyticsCategory.CATALOG),
            Map.entry("product.viewed",             AnalyticsCategory.CATALOG),
            Map.entry("product.added_to_cart",      AnalyticsCategory.CATALOG),
            Map.entry("inventory.adjusted",         AnalyticsCategory.INVENTORY),
            Map.entry("checkout.started",           AnalyticsCategory.CHECKOUT),
            Map.entry("checkout.completed",         AnalyticsCategory.CHECKOUT),
            Map.entry("order.created",              AnalyticsCategory.ORDER),
            Map.entry("order.delivered",            AnalyticsCategory.ORDER),
            Map.entry("order.cancelled",            AnalyticsCategory.ORDER),
            Map.entry("payment.captured",           AnalyticsCategory.PAYMENT),
            Map.entry("payment.failed",             AnalyticsCategory.PAYMENT),
            Map.entry("refund.requested",           AnalyticsCategory.REFUND),
            Map.entry("refund.completed",           AnalyticsCategory.REFUND),
            Map.entry("commission.accrued",         AnalyticsCategory.PAYOUT),
            Map.entry("settlement.released",        AnalyticsCategory.PAYOUT),
            Map.entry("payout.completed",           AnalyticsCategory.PAYOUT),
            Map.entry("notification.delivered",     AnalyticsCategory.SYSTEM),
            Map.entry("audit.record_created",       AnalyticsCategory.SYSTEM)
    );

    private static final Map<String, String[]> METRICS = Map.ofEntries(
            Map.entry("auth.user_registered",   new String[]{"customer.registrations"}),
            Map.entry("auth.user_logged_in",    new String[]{"customer.logins"}),
            Map.entry("vendor.applied",         new String[]{"vendor.applications"}),
            Map.entry("vendor.approved",        new String[]{"vendor.approvals"}),
            Map.entry("product.created",        new String[]{"catalog.products_created"}),
            Map.entry("product.approved",       new String[]{"catalog.products_approved"}),
            Map.entry("product.viewed",         new String[]{"catalog.product_views"}),
            Map.entry("checkout.started",       new String[]{"checkout.started"}),
            Map.entry("checkout.completed",     new String[]{"checkout.completed"}),
            Map.entry("order.created",          new String[]{"order.created", "order.gmv"}),
            Map.entry("order.delivered",        new String[]{"order.delivered"}),
            Map.entry("order.cancelled",        new String[]{"order.cancelled"}),
            Map.entry("payment.captured",       new String[]{"payment.captured.count", "payment.captured.amount"}),
            Map.entry("payment.failed",         new String[]{"payment.failed"}),
            Map.entry("refund.requested",       new String[]{"refund.requested"}),
            Map.entry("refund.completed",       new String[]{"refund.completed.count", "refund.completed.amount"}),
            Map.entry("commission.accrued",     new String[]{"commission.accrued"}),
            Map.entry("settlement.released",    new String[]{"settlement.released.amount"}),
            Map.entry("payout.completed",       new String[]{"payout.completed.count", "payout.completed.amount"}),
            Map.entry("notification.delivered", new String[]{"notification.delivered"}),
            Map.entry("audit.record_created",   new String[]{"audit.records"})
    );

    private static final java.util.Set<String> AMOUNT_METRICS = java.util.Set.of(
            "order.gmv", "payment.captured.amount", "refund.completed.amount",
            "commission.accrued", "settlement.released.amount", "payout.completed.amount"
    );

    public static AnalyticsCategory categoryOf(String eventType) {
        if (eventType == null) return null;
        return CATEGORY.get(eventType);
    }

    public static String[] metricsFor(String eventType) {
        if (eventType == null) return new String[0];
        return METRICS.getOrDefault(eventType, new String[0]);
    }

    public static boolean isAmountMetric(String metricCode) {
        return AMOUNT_METRICS.contains(metricCode);
    }
}