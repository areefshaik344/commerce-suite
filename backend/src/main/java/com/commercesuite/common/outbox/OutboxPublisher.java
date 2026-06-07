package com.commercesuite.common.outbox;

import com.commercesuite.common.audit.ActorContextHolder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Façade for domain code — adds correlation + actor headers. MUST run inside a TX. */
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxService outbox;
    private final ActorContextHolder actorHolder;

    public void publish(String aggregateType, String aggregateId,
                        String eventType, Object payload) {
        publish(aggregateType, aggregateId, eventType, payload, null);
    }

    public void publish(String aggregateType, String aggregateId,
                        String eventType, Object payload, String correlationId) {
        var actor = actorHolder.current();
        Map<String, String> headers = new HashMap<>();
        headers.put("actorId", actor != null && actor.userId() != null ? actor.userId().toString() : "");
        headers.put("requestId", actor != null && actor.requestId() != null ? actor.requestId() : "");
        headers.put("schemaVersion", "1");
        String cid = correlationId != null ? correlationId :
                (actor != null && actor.requestId() != null ? actor.requestId() : UUID.randomUUID().toString());
        outbox.record(aggregateType, aggregateId, eventType, payload, headers, cid);
    }
}