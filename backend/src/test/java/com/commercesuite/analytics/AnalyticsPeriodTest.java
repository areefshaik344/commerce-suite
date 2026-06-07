package com.commercesuite.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.analytics.domain.AnalyticsPeriod;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Pure unit test — no Spring context. Bucket boundaries must be stable. */
class AnalyticsPeriodTest {

    @Test
    void day_bucket_truncates_to_midnight_utc() {
        Instant at = Instant.parse("2026-06-07T13:42:11Z");
        assertThat(AnalyticsPeriod.DAY.bucketStart(at))
                .isEqualTo(Instant.parse("2026-06-07T00:00:00Z"));
        assertThat(AnalyticsPeriod.DAY.bucketEnd(at))
                .isEqualTo(Instant.parse("2026-06-08T00:00:00Z"));
    }

    @Test
    void week_bucket_starts_on_monday() {
        // 2026-06-07 is a Sunday → previous Monday is 2026-06-01
        Instant sunday = Instant.parse("2026-06-07T13:42:11Z");
        assertThat(AnalyticsPeriod.WEEK.bucketStart(sunday))
                .isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
    }

    @Test
    void month_bucket_starts_on_first_of_month() {
        Instant at = Instant.parse("2026-06-07T13:42:11Z");
        assertThat(AnalyticsPeriod.MONTH.bucketStart(at))
                .isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
    }

    @Test
    void lifetime_bucket_starts_at_epoch() {
        assertThat(AnalyticsPeriod.LIFETIME.bucketStart(Instant.now()))
                .isEqualTo(Instant.EPOCH);
    }
}