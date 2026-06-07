package com.commercesuite.common.outbox;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = """
        SELECT * FROM outbox_events
         WHERE status IN ('PENDING','FAILED')
           AND next_attempt_at <= :now
         ORDER BY next_attempt_at
         FOR UPDATE SKIP LOCKED
         LIMIT :limit
        """, nativeQuery = true)
    List<OutboxEvent> claimBatch(@Param("now") Instant now, @Param("limit") int limit);

    long countByStatus(OutboxStatus status);
}