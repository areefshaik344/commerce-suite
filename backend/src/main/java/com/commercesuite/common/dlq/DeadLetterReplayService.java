package com.commercesuite.common.dlq;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-queues DEAD_LETTER rows for the outbox/notification/webhook subsystems.
 * Replay is idempotent: existing PK is preserved, attempt_count reset, status
 * flipped back to PENDING. Downstream consumers already enforce idempotency.
 */
@Service
public class DeadLetterReplayService {
    private final JdbcTemplate jdbc;
    public DeadLetterReplayService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public enum Channel { OUTBOX, NOTIFICATION, WEBHOOK }

    @Transactional
    public int replayAll(Channel channel) {
        return switch (channel) {
            case OUTBOX -> jdbc.update(
                "UPDATE public.outbox_events SET status='PENDING', attempt_count=0, " +
                "next_attempt_at=now(), last_error=NULL WHERE status='DEAD_LETTER'");
            case NOTIFICATION -> jdbc.update(
                "UPDATE public.notification_deliveries SET status='PENDING', attempt_count=0, " +
                "next_attempt_at=now() WHERE status='DEAD_LETTER'");
            case WEBHOOK -> jdbc.update(
                "UPDATE public.webhook_deliveries SET status='PENDING', attempt_count=0, " +
                "next_attempt_at=now() WHERE status='DEAD_LETTER'");
        };
    }

    @Transactional
    public int replayOne(Channel channel, String id) {
        return switch (channel) {
            case OUTBOX -> jdbc.update(
                "UPDATE public.outbox_events SET status='PENDING', attempt_count=0, " +
                "next_attempt_at=now(), last_error=NULL WHERE id=?::uuid AND status='DEAD_LETTER'", id);
            case NOTIFICATION -> jdbc.update(
                "UPDATE public.notification_deliveries SET status='PENDING', attempt_count=0, " +
                "next_attempt_at=now() WHERE id=?::uuid AND status='DEAD_LETTER'", id);
            case WEBHOOK -> jdbc.update(
                "UPDATE public.webhook_deliveries SET status='PENDING', attempt_count=0, " +
                "next_attempt_at=now() WHERE id=?::uuid AND status='DEAD_LETTER'", id);
        };
    }

    public long count(Channel channel) {
        String table = switch (channel) {
            case OUTBOX -> "outbox_events";
            case NOTIFICATION -> "notification_deliveries";
            case WEBHOOK -> "webhook_deliveries";
        };
        Long c = jdbc.queryForObject(
            "SELECT count(*) FROM public." + table + " WHERE status='DEAD_LETTER'", Long.class);
        return c == null ? 0 : c;
    }
}
