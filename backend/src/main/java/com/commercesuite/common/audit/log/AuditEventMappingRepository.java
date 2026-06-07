package com.commercesuite.common.audit.log;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventMappingRepository extends JpaRepository<AuditEventMapping, UUID> {
    Optional<AuditEventMapping> findByEventType(String eventType);
    List<AuditEventMapping> findByEnabledTrue();
    List<AuditEventMapping> findByCategory(AuditCategory category);
}