/**
 * Idempotency-key contract for unsafe operations (payments, refunds, order
 * placement). Keys MUST be stable across retries of the same logical action.
 *
 * The backend stores `(actorId, idempotencyKey) -> response` for 24h and
 * replays the cached response on duplicate submission.
 */
import { newRequestId } from "./requestId";

export const IDEMPOTENCY_HEADER = "Idempotency-Key";
export const IDEMPOTENCY_TTL_HOURS = 24;

/** Scope-prefixed key so different operations cannot collide. */
export function newIdempotencyKey(scope: string): string {
  return `${scope}:${newRequestId()}`;
}

/** Deterministic key derived from an entity — safe for natural retries. */
export function stableIdempotencyKey(scope: string, entityId: string, intentVersion: number | string = 1): string {
  return `${scope}:${entityId}:${intentVersion}`;
}