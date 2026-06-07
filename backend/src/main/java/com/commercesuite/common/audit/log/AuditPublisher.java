package com.commercesuite.common.audit.log;

import com.commercesuite.auth.event.AuthEvents;
import com.commercesuite.common.outbox.OutboxDispatchEvent;
import com.commercesuite.common.outbox.OutboxEvent;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens for dispatched outbox events and produces an audit_log row
 * for the subset of events that BUSINESS_RULES.md flags as auditable.
 *
 * Subscribers run inside the dispatcher's transaction so the audit row
 * commits atomically with the outbox state change.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditPublisher {

    private final AuditService audit;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void onDispatched(OutboxDispatchEvent ev) {
        OutboxEvent o = ev.event();
        AuditAction action = mapAction(o.getEventType());
        if (action == null) return; // not auditable
        AuditActorType actorType = inferActor(action);
        AuditSeverity severity = mapSeverity(action);
        audit.record(new AuditContext(
                tryUuid(o.getHeaders(), "actorId"),
                actorType,
                o.getAggregateType(),
                o.getAggregateId(),
                action,
                severity,
                Map.of("eventId", o.getId().toString(),
                       "eventType", o.getEventType()),
                null,
                o.getCorrelationId(),
                null, null));
    }

    private static AuditAction mapAction(String type) {
        return switch (type) {
            case AuthEvents.USER_REGISTERED           -> AuditAction.USER_REGISTERED;
            case AuthEvents.USER_LOGGED_IN            -> AuditAction.USER_LOGGED_IN;
            case AuthEvents.USER_LOGGED_OUT           -> AuditAction.USER_LOGGED_OUT;
            case AuthEvents.PASSWORD_CHANGED          -> AuditAction.PASSWORD_CHANGED;
            case AuthEvents.PASSWORD_RESET_REQUESTED  -> AuditAction.PASSWORD_RESET_REQUESTED;
            case AuthEvents.PASSWORD_RESET_COMPLETED  -> AuditAction.PASSWORD_RESET_COMPLETED;
            case AuthEvents.EMAIL_VERIFIED            -> AuditAction.EMAIL_VERIFIED;
            case AuthEvents.REFRESH_TOKEN_REUSED      -> AuditAction.REFRESH_TOKEN_REUSED;
            case "vendor.approved"                    -> AuditAction.VENDOR_APPROVED;
            case "vendor.rejected"                    -> AuditAction.VENDOR_REJECTED;
            case "product.approved"                   -> AuditAction.PRODUCT_APPROVED;
            case "product.rejected"                   -> AuditAction.PRODUCT_REJECTED;
            case "inventory.adjusted"                 -> AuditAction.INVENTORY_ADJUSTED;
            case "order.created"                      -> AuditAction.ORDER_CREATED;
            case "order.cancelled"                    -> AuditAction.ORDER_CANCELLED;
            case "refund.approved"                    -> AuditAction.REFUND_APPROVED;
            case "settlement.locked"                  -> AuditAction.SETTLEMENT_LOCKED;
            case "payout.completed"                   -> AuditAction.PAYOUT_COMPLETED;
            default                                   -> null;
        };
    }

    private static AuditActorType inferActor(AuditAction a) {
        return switch (a) {
            case VENDOR_APPROVED, VENDOR_REJECTED, VENDOR_SUSPENDED,
                 PRODUCT_APPROVED, PRODUCT_REJECTED, REFUND_APPROVED,
                 REFUND_REJECTED, SETTLEMENT_LOCKED, PAYOUT_COMPLETED,
                 ADMIN_OVERRIDE                    -> AuditActorType.ADMIN;
            case INVENTORY_ADJUSTED                -> AuditActorType.VENDOR;
            case USER_REGISTERED, USER_LOGGED_IN,
                 USER_LOGGED_OUT, PASSWORD_CHANGED,
                 PASSWORD_RESET_REQUESTED,
                 PASSWORD_RESET_COMPLETED,
                 EMAIL_VERIFIED                    -> AuditActorType.USER;
            case REFRESH_TOKEN_REUSED,
                 SECURITY_VIOLATION                -> AuditActorType.SYSTEM;
            default                                -> AuditActorType.SYSTEM;
        };
    }

    private static AuditSeverity mapSeverity(AuditAction a) {
        return switch (a) {
            case REFRESH_TOKEN_REUSED, SECURITY_VIOLATION -> AuditSeverity.CRITICAL;
            case VENDOR_SUSPENDED, REFUND_REJECTED, PRODUCT_REJECTED -> AuditSeverity.WARNING;
            default -> AuditSeverity.INFO;
        };
    }

    private static UUID tryUuid(String headersJson, String key) {
        if (headersJson == null) return null;
        int idx = headersJson.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int colon = headersJson.indexOf(':', idx);
        int q1 = headersJson.indexOf('"', colon + 1);
        int q2 = headersJson.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return null;
        String raw = headersJson.substring(q1 + 1, q2);
        try { return raw.isEmpty() ? null : UUID.fromString(raw); }
        catch (IllegalArgumentException ex) { return null; }
    }
}