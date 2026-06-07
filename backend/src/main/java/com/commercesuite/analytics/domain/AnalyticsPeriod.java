package com.commercesuite.analytics.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/** Rollup bucket size. Mirrors V016 enum. */
public enum AnalyticsPeriod {
    HOUR, DAY, WEEK, MONTH, LIFETIME;

    /** Truncates the given instant to the start of its bucket. */
    public Instant bucketStart(Instant at) {
        var z = at.atZone(ZoneOffset.UTC);
        return switch (this) {
            case HOUR     -> z.truncatedTo(ChronoUnit.HOURS).toInstant();
            case DAY      -> z.truncatedTo(ChronoUnit.DAYS).toInstant();
            case WEEK     -> z.toLocalDate().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                              .atStartOfDay(ZoneOffset.UTC).toInstant();
            case MONTH    -> z.toLocalDate().withDayOfMonth(1)
                              .atStartOfDay(ZoneOffset.UTC).toInstant();
            case LIFETIME -> Instant.EPOCH;
        };
    }

    public Instant bucketEnd(Instant at) {
        Instant start = bucketStart(at);
        return switch (this) {
            case HOUR     -> start.plus(1, ChronoUnit.HOURS);
            case DAY      -> start.plus(1, ChronoUnit.DAYS);
            case WEEK     -> start.plus(7, ChronoUnit.DAYS);
            case MONTH    -> start.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
            case LIFETIME -> Instant.parse("9999-12-31T23:59:59Z");
        };
    }
}