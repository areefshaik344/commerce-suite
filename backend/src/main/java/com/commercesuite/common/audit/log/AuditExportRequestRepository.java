package com.commercesuite.common.audit.log;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditExportRequestRepository extends JpaRepository<AuditExportRequest, UUID> {
    Page<AuditExportRequest> findByRequestedByOrderByCreatedAtDesc(UUID requestedBy, Pageable pageable);
}