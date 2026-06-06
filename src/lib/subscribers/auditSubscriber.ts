import { eventBus } from "@/lib/eventBus";
import { auditApi } from "@/api/auditApi";
import type { AuditAction, AuditActor, AuditEntityType, AuditSeverity } from "@/types/audit";
import type { AnyDomainEvent } from "@/types/events";

let installed = false;

interface Mapping {
  action: AuditAction;
  entityType: AuditEntityType;
  entityId: (ev: AnyDomainEvent) => string;
  severity?: AuditSeverity;
}

const MAP: Partial<Record<AnyDomainEvent["type"], Mapping>> = {
  ORDER_CREATED:    { action: "order_placed",      entityType: "order",    entityId: (e) => (e.payload as { orderId: string }).orderId },
  ORDER_CANCELLED:  { action: "order_cancelled",   entityType: "order",    entityId: (e) => (e.payload as { orderId: string }).orderId, severity: "warning" },
  ORDER_DELIVERED:  { action: "order_delivered",   entityType: "order",    entityId: (e) => (e.payload as { orderId: string }).orderId },
  SHIPMENT_CREATED: { action: "created",           entityType: "shipment", entityId: (e) => (e.payload as { shipmentId: string }).shipmentId },
  SHIPMENT_UPDATED: { action: "updated",           entityType: "shipment", entityId: (e) => (e.payload as { shipmentId: string }).shipmentId },
  PAYMENT_CREATED:  { action: "created",           entityType: "payment",  entityId: (e) => (e.payload as { paymentId: string }).paymentId },
  PAYMENT_CAPTURED: { action: "payment_captured",  entityType: "payment",  entityId: (e) => (e.payload as { paymentId: string }).paymentId },
  PAYMENT_FAILED:   { action: "payment_failed",    entityType: "payment",  entityId: (e) => (e.payload as { paymentId: string }).paymentId, severity: "warning" },
  RETURN_REQUESTED: { action: "return_requested",  entityType: "return",   entityId: (e) => (e.payload as { returnId: string }).returnId },
  RETURN_APPROVED:  { action: "return_approved",   entityType: "return",   entityId: (e) => (e.payload as { returnId: string }).returnId },
  REFUND_ISSUED:    { action: "refund_issued",     entityType: "refund",   entityId: (e) => (e.payload as { refundId: string }).refundId },
  VENDOR_APPROVED:  { action: "approved",          entityType: "vendor",   entityId: (e) => (e.payload as { vendorId: string }).vendorId },
  USER_SUSPENDED:   { action: "suspended",         entityType: "user",     entityId: (e) => (e.payload as { userId: string }).userId, severity: "critical" },
};

function actorOf(ev: AnyDomainEvent): AuditActor {
  return ev.actor ?? { id: "system", role: "system" };
}

export function installAuditSubscriber(): void {
  if (installed) return;
  installed = true;
  eventBus.onAny((ev) => {
    const m = MAP[ev.type];
    if (!m) return;
    void auditApi.record({
      actor: actorOf(ev),
      action: m.action,
      entityType: m.entityType,
      entityId: m.entityId(ev),
      severity: m.severity ?? "info",
      correlationId: ev.correlationId,
      meta: { eventId: ev.id, source: ev.source },
    });
  });
}