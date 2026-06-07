/**
 * Reservation safety helpers — compensating release on:
 *   - checkout abandonment (route change away from /checkout)
 *   - payment failure
 *   - browser close (beforeunload)
 *   - reservation TTL expiry
 *
 * The actual reservation lives in `checkoutStore`. These helpers expose a
 * uniform contract the backend will mirror with a server-side TTL sweeper.
 */
import type { ActorContext } from "@/types/actor";

export const RESERVATION_TTL_MINUTES = 15;
export const RESERVATION_WARN_AT_SECONDS = 120; // warn 2min before expiry

export type ReservationReleaseReason =
  | "ABANDONED"
  | "PAYMENT_FAILED"
  | "PAYMENT_CANCELLED"
  | "TTL_EXPIRED"
  | "EXPLICIT_RELEASE"
  | "USER_LOGOUT";

export interface ReleaseReservationRequest {
  reservationId: string;
  reason: ReservationReleaseReason;
  actor: ActorContext;
}

export interface ReservationStatus {
  id: string;
  reservedAt: string;
  expiresAt: string;
  released: boolean;
  releaseReason?: ReservationReleaseReason;
}

/** Compute remaining seconds; returns 0 once expired. */
export function remainingReservationSeconds(expiresAt: string, now: Date = new Date()): number {
  const ms = new Date(expiresAt).getTime() - now.getTime();
  return Math.max(0, Math.floor(ms / 1000));
}

export function isReservationExpired(expiresAt: string, now: Date = new Date()): boolean {
  return remainingReservationSeconds(expiresAt, now) <= 0;
}