/**
 * Audit log domain — backend-ready records.
 *
 * Records are append-only. The frontend buffers locally and the backend
 * will persist via the same DTO shape.
 */

export type AuditActorRole = "customer" | "vendor" | "admin" | "system";

export type AuditEntityType =
  | "user"
  | "vendor"
  | "product"
  | "order"
  | "shipment"
  | "payment"
  | "refund"
  | "return"
  | "coupon"
  | "category"
  | "review"
  | "settings";

export type AuditAction =
  /** generic CRUD */
  | "created" | "updated" | "deleted" | "archived"
  /** auth & lifecycle */
  | "login" | "logout" | "password_changed" | "profile_updated"
  | "suspended" | "reinstated"
  /** moderation */
  | "approved" | "rejected" | "flagged"
  /** commerce */
  | "order_placed" | "order_cancelled" | "order_delivered"
  | "payment_captured" | "payment_failed"
  | "refund_issued" | "return_requested" | "return_approved";

export type AuditSeverity = "info" | "warning" | "critical";

export interface AuditActor {
  id: string;
  role: AuditActorRole;
  /** Display label (email, name). */
  label?: string;
}

export interface AuditRecord {
  id: string;
  actor: AuditActor;
  action: AuditAction;
  entityType: AuditEntityType;
  entityId: string;
  /** ISO timestamp. */
  timestamp: string;
  severity: AuditSeverity;
  /** Human-readable description. */
  message?: string;
  /** Arbitrary diff/context. */
  meta?: Record<string, unknown>;
  /** Correlation with the domain event that produced this record. */
  correlationId?: string;
}