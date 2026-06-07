package com.commercesuite.common.audit.log;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRetentionPolicyRepository extends JpaRepository<AuditRetentionPolicy, UUID> {
    Optional<AuditRetentionPolicy> findByCategory(AuditCategory category);
}