package com.commercesuite.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Single Clock bean — services MUST inject this instead of calling Instant.now() directly. */
@Configuration
public class ClockConfig {
    @Bean
    public Clock clock() { return Clock.systemUTC(); }
}
