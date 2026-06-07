package com.commercesuite.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persists outbox rows in the SAME transaction as the business write. */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repo;
    private final OutboxRetryPolicy retryPolicy;
    private final ObjectMapper mapper;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent record(String aggregateType,
                              String aggregateId,
                              String eventType,
                              Object payload,
                              Map<String, String> headers,
                              String correlationId) {
        OutboxEvent e = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(serialize(payload))
                .headers(serialize(headers == null ? Map.of() : headers))
                .status(OutboxStatus.PENDING)
                .attemptCount(0)
                .maxAttempts(retryPolicy.defaultMaxAttempts())
                .nextAttemptAt(Instant.now(clock))
                .correlationId(correlationId)
                .build();
        return repo.save(e);
    }

    private String serialize(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("outbox payload serialization failed", ex);
        }
    }
}