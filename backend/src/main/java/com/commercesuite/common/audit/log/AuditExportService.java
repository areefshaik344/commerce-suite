package com.commercesuite.common.audit.log;

import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.common.outbox.OutboxPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records audit-export intents and emits {@code audit.export_requested}
 * through the durable outbox. This phase persists metadata only — file
 * generation is a follow-up sprint.
 */
@Service
@RequiredArgsConstructor
public class AuditExportService {

    private final AuditExportRequestRepository repo;
    private final OutboxPublisher outbox;
    private final ActorContextHolder actors;
    private final ObjectMapper mapper;

    @Transactional
    public AuditExportRequest request(AuditExportFormat format,
                                      AuditSearchCriteria criteria) {
        var actor = actors.current();
        UUID requestedBy = actor != null ? actor.userId() : null;
        if (requestedBy == null) {
            throw new IllegalStateException("AuditExportService requires an authenticated actor");
        }
        AuditExportRequest row = AuditExportRequest.builder()
                .requestedBy(requestedBy)
                .format(format)
                .status(AuditExportStatus.PENDING)
                .criteria(serialize(toMap(criteria)))
                .build();
        AuditExportRequest saved = repo.save(row);

        outbox.publish(AuditEvents.AGGREGATE, saved.getId().toString(),
                AuditEvents.EXPORT_REQUESTED,
                new AuditEvents.ExportRequestedPayload(
                        saved.getId(), requestedBy, format.name(),
                        toMap(criteria), saved.getCreatedAt()));
        return saved;
    }

    private static Map<String, Object> toMap(AuditSearchCriteria c) {
        return Map.of(
                "actorId",     c.actorId()     == null ? "" : c.actorId().toString(),
                "entityType",  nz(c.entityType()),
                "entityId",    nz(c.entityId()),
                "action",      nz(c.action()),
                "category",    c.category()    == null ? "" : c.category().name(),
                "minSeverity", c.minSeverity() == null ? "" : c.minSeverity().name(),
                "from",        c.from()        == null ? "" : c.from().toString(),
                "to",          c.to()          == null ? "" : c.to().toString(),
                "requestId",   nz(c.requestId()));
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private String serialize(Map<String, Object> m) {
        try { return mapper.writeValueAsString(m); }
        catch (JsonProcessingException ex) { return "{}"; }
    }
}