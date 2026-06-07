package com.commercesuite.common.outbox;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventAttemptRepository extends JpaRepository<OutboxEventAttempt, UUID> {
}