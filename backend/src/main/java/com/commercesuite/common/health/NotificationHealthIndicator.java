package com.commercesuite.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("notifications")
public class NotificationHealthIndicator implements HealthIndicator {
    private final JdbcTemplate jdbc;
    public NotificationHealthIndicator(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override
    public Health health() {
        try {
            Long dead = jdbc.queryForObject(
                "SELECT count(*) FROM public.notification_deliveries WHERE status = 'DEAD_LETTER'", Long.class);
            return (dead != null && dead > 50 ? Health.down() : Health.up())
                    .withDetail("deadLetter", dead == null ? 0 : dead).build();
        } catch (Exception e) { return Health.unknown().withException(e).build(); }
    }
}
