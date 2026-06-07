package com.commercesuite.common.audit.log;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId, Pageable pageable);
    long countByAction(String action);
}