package com.commercesuite.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MetricSeriesPointDto(Instant bucketStart, long count, BigDecimal sum) {}