package com.commercesuite.analytics.retention;

import com.commercesuite.analytics.domain.AnalyticsCategory;
import java.util.EnumMap;
import java.util.Map;

/**
 * Declarative retention SLAs for analytics data. No purge job runs in
 * Phase 8.4 — this is the single source of truth for future jobs.
 */
public final class AnalyticsRetentionPolicy {
    private AnalyticsRetentionPolicy() {}

    public static final int RAW_EVENT_DAYS   = 365;
    public static final int AGGREGATION_DAYS = 365 * 5;
    public static final int SNAPSHOT_DAYS    = 365 * 7;

    private static final Map<AnalyticsCategory, Integer> RAW_BY_CATEGORY = new EnumMap<>(AnalyticsCategory.class);
    static {
        RAW_BY_CATEGORY.put(AnalyticsCategory.PAYMENT, 365 * 7);
        RAW_BY_CATEGORY.put(AnalyticsCategory.REFUND,  365 * 7);
        RAW_BY_CATEGORY.put(AnalyticsCategory.PAYOUT,  365 * 7);
        RAW_BY_CATEGORY.put(AnalyticsCategory.ORDER,   365 * 7);
    }

    public static int rawRetentionDays(AnalyticsCategory category) {
        return RAW_BY_CATEGORY.getOrDefault(category, RAW_EVENT_DAYS);
    }
}