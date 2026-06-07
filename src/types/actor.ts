/**
 * Actor identity contract — every mutating API call MUST carry an ActorContext.
 *
 * Backend will re-derive actor from the JWT; the frontend value is advisory only
 * (used for client-side audit/event correlation). DO NOT trust it server-side.
 */
export type ActorRole = "CUSTOMER" | "VENDOR" | "ADMIN" | "SYSTEM";

export interface ActorContext {
  actorId: string;
  actorRole: ActorRole;
  /** Optional sub-role for multi-role users (e.g. vendor who is also a customer). */
  activeRole?: ActorRole;
  /** Correlates client → API → audit → webhook. Echoed back as X-Request-Id. */
  requestId: string;
  /** Idempotency key for safe retry of POSTs. Required on payment/refund mutations. */
  idempotencyKey?: string;
}

/** Helper type — every write DTO should extend this. */
export type WithActor<T> = T & { actor: ActorContext };

export interface OwnershipAssertion {
  resourceType: "ORDER" | "PRODUCT" | "REVIEW" | "ADDRESS" | "PAYOUT" | "VENDOR" | "CART";
  resourceId: string;
  ownerId: string;
}