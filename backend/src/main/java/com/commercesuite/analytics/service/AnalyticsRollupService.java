package com.commercesuite.analytics.service;

import com.commercesuite.analytics.repository.AnalyticsAggregationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Periodic housekeeping for analytics rollups. Phase 8.4 keeps this
 * lightweight — incremental updates run in {@link AnalyticsAggregator}.
 * Reserved for back-fill / snapshot capture jobs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsRollupService {

    private final AnalyticsAggregationRepository aggRepo;

    @Value("${analytics.rollup.enabled:false}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${analytics.rollup.delay-ms:300000}")
    public void heartbeat() {
        if (!enabled) return;
        log.debug("[analytics-rollup] aggregations stored={}", aggRepo.count());
    }
}