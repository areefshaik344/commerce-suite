/**
 * GDPR / DPDP compliance contract — account deletion + data export.
 *
 * Frontend exposes intent; backend performs the actual purge / package.
 */
import type { ActorContext } from "@/types/actor";

export type DeletionReason =
  | "USER_REQUEST"
  | "INACTIVITY"
  | "FRAUD"
  | "DUPLICATE_ACCOUNT"
  | "OTHER";

export interface AccountDeletionRequest {
  actor: ActorContext;
  reason: DeletionReason;
  /** ISO timestamp — request is hard-deleted after this date. Default 30d grace. */
  scheduledFor: string;
  /** Free-form note shown in admin audit log. */
  note?: string;
}

export interface AccountDeletionStatus {
  requestId: string;
  userId: string;
  status: "REQUESTED" | "GRACE_PERIOD" | "PROCESSING" | "COMPLETED" | "CANCELLED";
  requestedAt: string;
  scheduledFor: string;
  completedAt?: string;
}

export interface DataExportRequest {
  actor: ActorContext;
  /** Domains the user opts into exporting. */
  include: ReadonlyArray<
    "PROFILE" | "ORDERS" | "REVIEWS" | "ADDRESSES" | "PAYMENTS" | "NOTIFICATIONS"
  >;
  format: "JSON" | "CSV";
}

export interface DataExportArtifact {
  id: string;
  userId: string;
  url: string;             // pre-signed, expires in 24h
  expiresAt: string;
  bytes: number;
  format: "JSON" | "CSV";
  createdAt: string;
}

export const GDPR_GRACE_PERIOD_DAYS = 30;

export function defaultDeletionDate(now: Date = new Date()): string {
  const d = new Date(now);
  d.setDate(d.getDate() + GDPR_GRACE_PERIOD_DAYS);
  return d.toISOString();
}