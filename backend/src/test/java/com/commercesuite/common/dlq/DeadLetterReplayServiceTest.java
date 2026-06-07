package com.commercesuite.common.dlq;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DeadLetterReplayServiceTest {
    @Test void replayAllOutboxIssuesExpectedSql() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(contains("outbox_events"))).thenReturn(5);
        DeadLetterReplayService svc = new DeadLetterReplayService(jdbc);
        assertEquals(5, svc.replayAll(DeadLetterReplayService.Channel.OUTBOX));
    }
}
