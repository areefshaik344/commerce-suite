/**
 * Finite State Machine registry — formalizes transition rules and role-based
 * guards for every long-lived domain entity (Order, Shipment, Return, Payment,
 * Refund, Payout).
 *
 * Pure & side-effect free. UI and stores call `canTransition` before issuing
 * a mutation; the backend re-enforces the same rules server-side.
 */
import type { ActorRole } from "@/types/actor";
import {
  ORDER_STATUS, SHIPMENT_STATUS, RETURN_STATUS, PAYMENT_STATUS,
  type OrderStatus, type ShipmentStatus, type ReturnStatus,
} from "@/types/order";
import { PAYMENT_INTENT_STATUS, type PaymentIntentStatus } from "@/types/payment";

export interface TransitionRule<S extends string> {
  from: S;
  to: S;
  /** Roles permitted to trigger this transition. */
  allowedRoles: ReadonlyArray<ActorRole>;
  /** Optional human-readable reason — surfaced in audit logs. */
  reason?: string;
}

export interface Fsm<S extends string> {
  name: string;
  initial: S;
  terminal: ReadonlyArray<S>;
  transitions: ReadonlyArray<TransitionRule<S>>;
}

export function canTransition<S extends string>(
  fsm: Fsm<S>,
  from: S,
  to: S,
  role: ActorRole,
): boolean {
  return fsm.transitions.some(
    (t) => t.from === from && t.to === to && t.allowedRoles.includes(role),
  );
}

export function nextStates<S extends string>(fsm: Fsm<S>, from: S, role?: ActorRole): S[] {
  return fsm.transitions
    .filter((t) => t.from === from && (!role || t.allowedRoles.includes(role)))
    .map((t) => t.to);
}

export function isTerminal<S extends string>(fsm: Fsm<S>, state: S): boolean {
  return fsm.terminal.includes(state);
}

/** Assertion helper — throw early in stores when a transition is illegal. */
export class InvalidTransitionError extends Error {
  constructor(public fsmName: string, public from: string, public to: string, public role: ActorRole) {
    super(`[${fsmName}] illegal transition ${from} -> ${to} by ${role}`);
    this.name = "InvalidTransitionError";
  }
}

export function assertTransition<S extends string>(fsm: Fsm<S>, from: S, to: S, role: ActorRole): void {
  if (!canTransition(fsm, from, to, role)) {
    throw new InvalidTransitionError(fsm.name, from as string, to as string, role);
  }
}

/* -------------------------------------------------------------------------- */
/* Order FSM                                                                  */
/* -------------------------------------------------------------------------- */
const C: ActorRole = "CUSTOMER";
const V: ActorRole = "VENDOR";
const A: ActorRole = "ADMIN";
const S: ActorRole = "SYSTEM";

export const orderFsm: Fsm<OrderStatus> = {
  name: "Order",
  initial: ORDER_STATUS.CREATED,
  terminal: [ORDER_STATUS.DELIVERED, ORDER_STATUS.REFUNDED],
  transitions: [
    { from: "CREATED",           to: "CONFIRMED",         allowedRoles: [V, A, S] },
    { from: "CREATED",           to: "CANCELLED",         allowedRoles: [C, V, A, S] },
    { from: "CONFIRMED",         to: "PROCESSING",        allowedRoles: [V, A, S] },
    { from: "CONFIRMED",         to: "CANCELLED",         allowedRoles: [C, V, A] },
    { from: "PROCESSING",        to: "PARTIALLY_SHIPPED", allowedRoles: [V, A, S] },
    { from: "PROCESSING",        to: "SHIPPED",           allowedRoles: [V, A, S] },
    { from: "PROCESSING",        to: "CANCELLED",         allowedRoles: [V, A] },
    { from: "PARTIALLY_SHIPPED", to: "SHIPPED",           allowedRoles: [V, A, S] },
    { from: "PARTIALLY_SHIPPED", to: "DELIVERED",         allowedRoles: [S, A] },
    { from: "SHIPPED",           to: "DELIVERED",         allowedRoles: [S, A] },
    { from: "DELIVERED",         to: "RETURNED",          allowedRoles: [C, A, S] },
    { from: "CANCELLED",         to: "REFUNDED",          allowedRoles: [A, S] },
    { from: "RETURNED",          to: "REFUNDED",          allowedRoles: [A, S] },
  ],
};

/* -------------------------------------------------------------------------- */
/* Shipment FSM                                                               */
/* -------------------------------------------------------------------------- */
export const shipmentFsm: Fsm<ShipmentStatus> = {
  name: "Shipment",
  initial: SHIPMENT_STATUS.PACKING,
  terminal: [SHIPMENT_STATUS.DELIVERED],
  transitions: [
    { from: "PACKING",          to: "READY_TO_SHIP",    allowedRoles: [V, A] },
    { from: "READY_TO_SHIP",    to: "IN_TRANSIT",       allowedRoles: [V, A, S] },
    { from: "IN_TRANSIT",       to: "OUT_FOR_DELIVERY", allowedRoles: [S, A] },
    { from: "IN_TRANSIT",       to: "FAILED_DELIVERY",  allowedRoles: [S, A] },
    { from: "OUT_FOR_DELIVERY", to: "DELIVERED",        allowedRoles: [S, A] },
    { from: "OUT_FOR_DELIVERY", to: "FAILED_DELIVERY",  allowedRoles: [S, A] },
    { from: "FAILED_DELIVERY",  to: "OUT_FOR_DELIVERY", allowedRoles: [S, A] },
    { from: "FAILED_DELIVERY",  to: "READY_TO_SHIP",    allowedRoles: [V, A] },
  ],
};

/* -------------------------------------------------------------------------- */
/* Return FSM                                                                 */
/* -------------------------------------------------------------------------- */
export const returnFsm: Fsm<ReturnStatus> = {
  name: "Return",
  initial: RETURN_STATUS.REQUESTED,
  terminal: [RETURN_STATUS.REFUNDED, RETURN_STATUS.REJECTED],
  transitions: [
    { from: "REQUESTED", to: "APPROVED",  allowedRoles: [V, A] },
    { from: "REQUESTED", to: "REJECTED",  allowedRoles: [V, A] },
    { from: "APPROVED",  to: "PICKED_UP", allowedRoles: [V, A, S] },
    { from: "PICKED_UP", to: "REFUNDED",  allowedRoles: [A, S] },
  ],
};

/* -------------------------------------------------------------------------- */
/* Payment Intent FSM                                                         */
/* -------------------------------------------------------------------------- */
export const paymentFsm: Fsm<PaymentIntentStatus> = {
  name: "PaymentIntent",
  initial: PAYMENT_INTENT_STATUS.CREATED,
  terminal: [
    PAYMENT_INTENT_STATUS.CAPTURED,
    PAYMENT_INTENT_STATUS.CANCELLED,
    PAYMENT_INTENT_STATUS.REFUNDED,
    PAYMENT_INTENT_STATUS.FAILED,
  ],
  transitions: [
    { from: "CREATED",            to: "REQUIRES_ACTION",    allowedRoles: [S] },
    { from: "CREATED",            to: "AUTHORIZED",         allowedRoles: [S] },
    { from: "CREATED",            to: "FAILED",             allowedRoles: [S] },
    { from: "CREATED",            to: "CANCELLED",          allowedRoles: [C, A, S] },
    { from: "REQUIRES_ACTION",    to: "AUTHORIZED",         allowedRoles: [S] },
    { from: "REQUIRES_ACTION",    to: "FAILED",             allowedRoles: [S] },
    { from: "REQUIRES_ACTION",    to: "CANCELLED",          allowedRoles: [C, A, S] },
    { from: "AUTHORIZED",         to: "CAPTURED",           allowedRoles: [S, A] },
    { from: "AUTHORIZED",         to: "CANCELLED",          allowedRoles: [A, S] },
    { from: "CAPTURED",           to: "PARTIALLY_REFUNDED", allowedRoles: [A, S] },
    { from: "CAPTURED",           to: "REFUNDED",           allowedRoles: [A, S] },
    { from: "PARTIALLY_REFUNDED", to: "REFUNDED",           allowedRoles: [A, S] },
    { from: "FAILED",             to: "CREATED",            allowedRoles: [C, S] }, // retry creates a new attempt
  ],
};

/* -------------------------------------------------------------------------- */
/* Refund FSM                                                                 */
/* -------------------------------------------------------------------------- */
export type RefundState = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export const refundFsm: Fsm<RefundState> = {
  name: "Refund",
  initial: "PENDING",
  terminal: ["COMPLETED", "FAILED"],
  transitions: [
    { from: "PENDING",    to: "PROCESSING", allowedRoles: [S, A] },
    { from: "PROCESSING", to: "COMPLETED",  allowedRoles: [S] },
    { from: "PROCESSING", to: "FAILED",     allowedRoles: [S] },
    { from: "FAILED",     to: "PENDING",    allowedRoles: [A] },
  ],
};

/* -------------------------------------------------------------------------- */
/* Payout FSM                                                                 */
/* -------------------------------------------------------------------------- */
export type PayoutState =
  | "ACCRUED"
  | "SCHEDULED"
  | "PROCESSING"
  | "PAID"
  | "FAILED"
  | "ON_HOLD"
  | "REVERSED";

export const payoutFsm: Fsm<PayoutState> = {
  name: "Payout",
  initial: "ACCRUED",
  terminal: ["PAID", "REVERSED"],
  transitions: [
    { from: "ACCRUED",    to: "SCHEDULED",  allowedRoles: [A, S] },
    { from: "ACCRUED",    to: "ON_HOLD",    allowedRoles: [A] },
    { from: "SCHEDULED",  to: "PROCESSING", allowedRoles: [S] },
    { from: "SCHEDULED",  to: "ON_HOLD",    allowedRoles: [A] },
    { from: "PROCESSING", to: "PAID",       allowedRoles: [S] },
    { from: "PROCESSING", to: "FAILED",     allowedRoles: [S] },
    { from: "FAILED",     to: "SCHEDULED",  allowedRoles: [A] },
    { from: "ON_HOLD",    to: "SCHEDULED",  allowedRoles: [A] },
    { from: "PAID",       to: "REVERSED",   allowedRoles: [A] },
  ],
};

export const FSMS = {
  order: orderFsm,
  shipment: shipmentFsm,
  return: returnFsm,
  payment: paymentFsm,
  refund: refundFsm,
  payout: payoutFsm,
} as const;