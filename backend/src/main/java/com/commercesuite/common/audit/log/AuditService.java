package com.commercesuite.common.audit.log;

import com.commercesuite.common.audit.ActorContextHolder;
import com.commercesuite.common.outbox.OutboxPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Append-only persistence façade for audit records. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repo;
    private final ActorContextHolder actorHolder;
    private final ObjectMapper mapper;
    private final OutboxPublisher outbox;

    @Transactional(propagation = Propagation.REQUIRED)
    public AuditLog record(AuditContext ctx) {
        var actor = actorHolder.current();
        UUID actorId = ctx.actorId() != null ? ctx.actorId()
                : (actor != null ? actor.userId() : null);
        String requestId = ctx.requestId() != null ? ctx.requestId()
                : (actor != null ? actor.requestId() : null);
        AuditLog row = AuditLog.builder()
                .actorId(actorId)
                .actorType(ctx.actorType().name())
                .entityType(ctx.entityType())
                .entityId(ctx.entityId())
                .action(ctx.action().name())
                .severity(ctx.severity() == null ? AuditSeverity.INFO : ctx.severity())
                .category(ctx.category() == null ? AuditCategory.SYSTEM : ctx.category())
                .metadata(serialize(ctx.metadata()))
                .requestId(requestId)
                .correlationId(ctx.correlationId())
                .ipAddress(ctx.ipAddress())
                .userAgent(ctx.userAgent())
                .build();
        AuditLog saved = repo.save(row);
        // Emit a domain event via the durable outbox so downstream
        // subscribers (SIEM, archival, etc.) can react asynchronously.
        outbox.publish("AUDIT_LOG", saved.getId().toString(),
                AuditEvents.RECORD_CREATED,
                new AuditEvents.RecordCreatedPayload(
                        saved.getId(), saved.getActorId(), saved.getEntityType(),
                        saved.getEntityId(), saved.getAction(),
                        saved.getCategory(), saved.getSeverity(),
                        saved.getCreatedAt()));
        return saved;
    }

    private String serialize(Map<String, Object> meta) {
        try { return mapper.writeValueAsString(meta == null ? Map.of() : meta); }
        catch (JsonProcessingException ex) { return "{}"; }
    }
}