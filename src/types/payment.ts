/**
 * Payment domain — backend-ready DTOs.
 *
 * Provider-agnostic. The frontend MUST NOT branch on gateway names;
 * branch on PaymentMethodKind instead. Money is integer ₹ throughout.
 */
import type { PaymentMethodId } from "./checkout";

export const PAYMENT_INTENT_STATUS = {
  CREATED: "CREATED",
  REQUIRES_ACTION: "REQUIRES_ACTION",
  AUTHORIZED: "AUTHORIZED",
  CAPTURED: "CAPTURED",
  FAILED: "FAILED",
  CANCELLED: "CANCELLED",
  REFUNDED: "REFUNDED",
  PARTIALLY_REFUNDED: "PARTIALLY_REFUNDED",
} as const;
export type PaymentIntentStatus = typeof PAYMENT_INTENT_STATUS[keyof typeof PAYMENT_INTENT_STATUS];

export const PAYMENT_ATTEMPT_STATUS = {
  PENDING: "PENDING",
  SUCCEEDED: "SUCCEEDED",
  FAILED: "FAILED",
  EXPIRED: "EXPIRED",
} as const;
export type PaymentAttemptStatus = typeof PAYMENT_ATTEMPT_STATUS[keyof typeof PAYMENT_ATTEMPT_STATUS];

export type PaymentMethodKind = "COD" | "UPI" | "CARD" | "WALLET" | "NETBANKING";

export interface PaymentMethod {
  id: PaymentMethodId;
  kind: PaymentMethodKind;
  label: string;
  gateway?: string;
  prepaid: boolean;
  enabled: boolean;
  display?: string;
  iconUrl?: string;
}

export interface PaymentAttempt {
  id: string;
  intentId: string;
  methodId: PaymentMethodId;
  status: PaymentAttemptStatus;
  failureCode?: string;
  failureMessage?: string;
  amount: number;
  startedAt: string;
  finishedAt: string | null;
}

export interface TransactionRecord {
  id: string;
  intentId: string;
  attemptId: string | null;
  kind: "AUTHORIZATION" | "CAPTURE" | "REFUND" | "VOID";
  amount: number;
  gatewayRef: string | null;
  at: string;
}

export interface PaymentIntent {
  id: string;
  orderId: string | null;
  idempotencyKey: string;
  amount: number;
  currency: "INR";
  methodId: PaymentMethodId;
  methodKind: PaymentMethodKind;
  status: PaymentIntentStatus;
  attemptCount: number;
  maxAttempts: number;
  expiresAt: string;
  capturedAmount: number;
  refundedAmount: number;
  attempts: PaymentAttempt[];
  transactions: TransactionRecord[];
  createdAt: string;
  updatedAt: string;
}

export interface RefundTransaction {
  id: string;
  intentId: string;
  orderId: string;
  amount: number;
  reason: string;
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
  sourceType: "CANCELLATION" | "RETURN" | "ADJUSTMENT";
  sourceId: string;
  createdAt: string;
  completedAt: string | null;
}

export const PAYMENT_METHODS_CATALOG: PaymentMethod[] = [
  { id: "upi",    kind: "UPI",    label: "UPI",               prepaid: true,  enabled: true },
  { id: "card",   kind: "CARD",   label: "Credit/Debit Card", prepaid: true,  enabled: true },
  { id: "wallet", kind: "WALLET", label: "Wallet",            prepaid: true,  enabled: true },
  { id: "cod",    kind: "COD",    label: "Cash on Delivery",  prepaid: false, enabled: true },
];

export const PAYMENT_RETRY_LIMIT = 3;
export const PAYMENT_INTENT_TTL_MINUTES = 15;
