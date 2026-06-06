/**
 * Platform domain event contracts.
 *
 * These are the canonical event shapes published across the marketplace.
 * They are transport-agnostic: today they flow through the in-process
 * EventBus, tomorrow the backend will emit identical payloads through
 * webhooks / message queues. DTO shape MUST stay stable.
 */

export type DomainEventType =
  | "ORDER_CREATED"
  | "ORDER_CANCELLED"
  | "ORDER_DELIVERED"
  | "SHIPMENT_CREATED"
  | "SHIPMENT_UPDATED"
  | "PAYMENT_CREATED"
  | "PAYMENT_CAPTURED"
  | "PAYMENT_FAILED"
  | "RETURN_REQUESTED"
  | "RETURN_APPROVED"
  | "REFUND_ISSUED"
  | "VENDOR_APPROVED"
  | "USER_SUSPENDED";

export interface DomainEventActor {
  id: string;
  role: "customer" | "vendor" | "admin" | "system";
}

export interface DomainEventEnvelope<T extends DomainEventType, P> {
  /** ULID-style event id, unique across the platform. */
  id: string;
  type: T;
  /** ISO timestamp, UTC. */
  occurredAt: string;
  /** Source domain (orders, payments, shipping, ...). */
  source: string;
  /** Schema version of the payload — bump on breaking changes. */
  version: 1;
  actor?: DomainEventActor;
  /** Correlation id to link related events (e.g. checkout → order → payment). */
  correlationId?: string;
  payload: P;
}

/* ---------------- Payload contracts ---------------- */

export interface OrderCreatedPayload {
  orderId: string;
  customerId: string;
  vendorIds: string[];
  total: number;
  currency: string;
  itemCount: number;
}
export interface OrderCancelledPayload {
  orderId: string;
  customerId: string;
  reason?: string;
  cancelledItemIds?: string[];
}
export interface OrderDeliveredPayload {
  orderId: string;
  customerId: string;
  deliveredAt: string;
}

export interface ShipmentCreatedPayload {
  shipmentId: string;
  orderId: string;
  vendorId: string;
  carrier?: string;
  trackingNumber?: string;
}
export interface ShipmentUpdatedPayload {
  shipmentId: string;
  orderId: string;
  status: string;
  location?: string;
}

export interface PaymentCreatedPayload {
  paymentId: string;
  orderId: string;
  amount: number;
  currency: string;
  method: string;
}
export interface PaymentCapturedPayload {
  paymentId: string;
  orderId: string;
  amount: number;
  currency: string;
}
export interface PaymentFailedPayload {
  paymentId: string;
  orderId: string;
  reason: string;
  retryable: boolean;
}

export interface ReturnRequestedPayload {
  returnId: string;
  orderId: string;
  customerId: string;
  itemIds: string[];
  reason: string;
}
export interface ReturnApprovedPayload {
  returnId: string;
  orderId: string;
  approvedBy: string;
}
export interface RefundIssuedPayload {
  refundId: string;
  orderId: string;
  amount: number;
  currency: string;
}

export interface VendorApprovedPayload {
  vendorId: string;
  approvedBy: string;
}
export interface UserSuspendedPayload {
  userId: string;
  suspendedBy: string;
  reason?: string;
}

/* ---------------- Typed event map ---------------- */

export type DomainEventMap = {
  ORDER_CREATED: DomainEventEnvelope<"ORDER_CREATED", OrderCreatedPayload>;
  ORDER_CANCELLED: DomainEventEnvelope<"ORDER_CANCELLED", OrderCancelledPayload>;
  ORDER_DELIVERED: DomainEventEnvelope<"ORDER_DELIVERED", OrderDeliveredPayload>;
  SHIPMENT_CREATED: DomainEventEnvelope<"SHIPMENT_CREATED", ShipmentCreatedPayload>;
  SHIPMENT_UPDATED: DomainEventEnvelope<"SHIPMENT_UPDATED", ShipmentUpdatedPayload>;
  PAYMENT_CREATED: DomainEventEnvelope<"PAYMENT_CREATED", PaymentCreatedPayload>;
  PAYMENT_CAPTURED: DomainEventEnvelope<"PAYMENT_CAPTURED", PaymentCapturedPayload>;
  PAYMENT_FAILED: DomainEventEnvelope<"PAYMENT_FAILED", PaymentFailedPayload>;
  RETURN_REQUESTED: DomainEventEnvelope<"RETURN_REQUESTED", ReturnRequestedPayload>;
  RETURN_APPROVED: DomainEventEnvelope<"RETURN_APPROVED", ReturnApprovedPayload>;
  REFUND_ISSUED: DomainEventEnvelope<"REFUND_ISSUED", RefundIssuedPayload>;
  VENDOR_APPROVED: DomainEventEnvelope<"VENDOR_APPROVED", VendorApprovedPayload>;
  USER_SUSPENDED: DomainEventEnvelope<"USER_SUSPENDED", UserSuspendedPayload>;
};

export type AnyDomainEvent = DomainEventMap[DomainEventType];

export type DomainEventHandler<T extends DomainEventType> = (
  event: DomainEventMap[T]
) => void | Promise<void>;

/* ---------------- Webhook DTO (backend-bound) ---------------- */

export interface WebhookEventDTO<T extends DomainEventType = DomainEventType> {
  id: string;
  type: T;
  occurredAt: string;
  source: string;
  version: 1;
  correlationId?: string;
  actor?: DomainEventActor;
  data: DomainEventMap[T]["payload"];
}

/** Convert an in-process envelope to the webhook delivery DTO. */
export function toWebhookDTO<T extends DomainEventType>(
  ev: DomainEventMap[T]
): WebhookEventDTO<T> {
  return {
    id: ev.id,
    type: ev.type,
    occurredAt: ev.occurredAt,
    source: ev.source,
    version: ev.version,
    correlationId: ev.correlationId,
    actor: ev.actor,
    data: ev.payload,
  };
}