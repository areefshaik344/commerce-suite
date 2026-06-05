import { simulateDelay, mockSuccess, ApiError, type ApiResponse } from "./apiClient";
import type {
  PaymentIntent, PaymentAttempt, RefundTransaction, PaymentMethod,
  PaymentMethodKind,
} from "@/types/payment";
import {
  PAYMENT_METHODS_CATALOG, PAYMENT_RETRY_LIMIT, PAYMENT_INTENT_TTL_MINUTES,
} from "@/types/payment";
import type { PaymentMethodId } from "@/types/checkout";

const INTENTS: Record<string, PaymentIntent> = {};
/** Idempotency map: idempotencyKey -> intentId. */
const IDEMPOTENT: Record<string, string> = {};
const REFUNDS: Record<string, RefundTransaction[]> = {};

const seq = (p: string) => `${p}-${Date.now().toString(36)}-${Math.floor(Math.random() * 1e4).toString(36)}`;
const now = () => new Date().toISOString();
const ttlExpiry = () => new Date(Date.now() + PAYMENT_INTENT_TTL_MINUTES * 60_000).toISOString();

function kindFor(id: PaymentMethodId): PaymentMethodKind {
  return PAYMENT_METHODS_CATALOG.find(m => m.id === id)?.kind ?? "CARD";
}

function ensureFresh(intent: PaymentIntent): PaymentIntent {
  if (intent.status === "CREATED" || intent.status === "REQUIRES_ACTION") {
    if (Date.now() > new Date(intent.expiresAt).getTime()) {
      intent.status = "FAILED";
      intent.updatedAt = now();
    }
  }
  return intent;
}

export const paymentApi = {
  async listMethods(): Promise<ApiResponse<PaymentMethod[]>> {
    await simulateDelay(100);
    return mockSuccess(PAYMENT_METHODS_CATALOG.filter(m => m.enabled));
  },

  async createIntent(input: {
    orderId?: string | null;
    amount: number;
    methodId: PaymentMethodId;
    idempotencyKey: string;
  }): Promise<ApiResponse<PaymentIntent>> {
    await simulateDelay(200);
    if (input.amount <= 0) throw new ApiError("Invalid amount", 400, "INVALID_AMOUNT");
    const existing = IDEMPOTENT[input.idempotencyKey];
    if (existing && INTENTS[existing]) return mockSuccess(ensureFresh(INTENTS[existing]));
    const kind = kindFor(input.methodId);
    const intent: PaymentIntent = {
      id: seq("PI"),
      orderId: input.orderId ?? null,
      idempotencyKey: input.idempotencyKey,
      amount: input.amount,
      currency: "INR",
      methodId: input.methodId,
      methodKind: kind,
      status: kind === "COD" ? "AUTHORIZED" : "CREATED",
      attemptCount: 0,
      maxAttempts: PAYMENT_RETRY_LIMIT,
      expiresAt: ttlExpiry(),
      capturedAmount: 0,
      refundedAmount: 0,
      attempts: [],
      transactions: [],
      createdAt: now(),
      updatedAt: now(),
    };
    INTENTS[intent.id] = intent;
    IDEMPOTENT[input.idempotencyKey] = intent.id;
    return mockSuccess(intent);
  },

  async getIntent(intentId: string): Promise<ApiResponse<PaymentIntent>> {
    await simulateDelay(120);
    const i = INTENTS[intentId];
    if (!i) throw new ApiError("Intent not found", 404, "INTENT_NOT_FOUND");
    return mockSuccess(ensureFresh(i));
  },

  /** Mock attempt — simulates either success or transient failure. */
  async confirm(input: { intentId: string; simulateOutcome?: "success" | "fail" }): Promise<ApiResponse<PaymentIntent>> {
    await simulateDelay(450);
    const intent = INTENTS[input.intentId];
    if (!intent) throw new ApiError("Intent not found", 404, "INTENT_NOT_FOUND");
    ensureFresh(intent);
    if (intent.status === "CAPTURED" || intent.status === "AUTHORIZED") {
      return mockSuccess(intent);
    }
    if (intent.status === "FAILED" && intent.attemptCount >= intent.maxAttempts) {
      throw new ApiError("Retry limit exceeded", 409, "RETRY_LIMIT");
    }
    if (Date.now() > new Date(intent.expiresAt).getTime()) {
      throw new ApiError("Payment intent expired", 410, "INTENT_EXPIRED");
    }
    const attempt: PaymentAttempt = {
      id: seq("PA"),
      intentId: intent.id,
      methodId: intent.methodId,
      status: "PENDING",
      amount: intent.amount,
      startedAt: now(),
      finishedAt: null,
    };
    intent.attempts.push(attempt);
    intent.attemptCount += 1;
    intent.updatedAt = now();

    // Outcome: COD always authorizes; otherwise honour simulateOutcome (default: success).
    const success = intent.methodKind === "COD"
      ? true
      : input.simulateOutcome === "fail" ? false : true;
    attempt.finishedAt = now();
    if (success) {
      attempt.status = "SUCCEEDED";
      intent.status = intent.methodKind === "COD" ? "AUTHORIZED" : "CAPTURED";
      intent.capturedAmount = intent.methodKind === "COD" ? 0 : intent.amount;
      intent.transactions.push({
        id: seq("TX"), intentId: intent.id, attemptId: attempt.id,
        kind: intent.methodKind === "COD" ? "AUTHORIZATION" : "CAPTURE",
        amount: intent.amount, gatewayRef: `GW-${seq("R")}`, at: now(),
      });
    } else {
      attempt.status = "FAILED";
      attempt.failureCode = "GATEWAY_DECLINED";
      attempt.failureMessage = "Issuer declined the transaction.";
      intent.status = "FAILED";
    }
    intent.updatedAt = now();
    return mockSuccess({ ...intent });
  },

  async cancel(intentId: string): Promise<ApiResponse<PaymentIntent>> {
    await simulateDelay(150);
    const intent = INTENTS[intentId];
    if (!intent) throw new ApiError("Intent not found", 404, "INTENT_NOT_FOUND");
    if (intent.status === "CAPTURED") throw new ApiError("Already captured", 409, "ALREADY_CAPTURED");
    intent.status = "CANCELLED";
    intent.updatedAt = now();
    return mockSuccess(intent);
  },

  async refund(input: {
    intentId: string; amount: number; reason: string;
    sourceType: RefundTransaction["sourceType"]; sourceId: string; orderId: string;
  }): Promise<ApiResponse<RefundTransaction>> {
    await simulateDelay(250);
    const intent = INTENTS[input.intentId];
    if (!intent) throw new ApiError("Intent not found", 404, "INTENT_NOT_FOUND");
    const remaining = intent.capturedAmount - intent.refundedAmount;
    if (input.amount <= 0 || input.amount > remaining) {
      throw new ApiError("Invalid refund amount", 400, "INVALID_REFUND");
    }
    const refund: RefundTransaction = {
      id: seq("RF"),
      intentId: intent.id,
      orderId: input.orderId,
      amount: input.amount,
      reason: input.reason,
      status: "PROCESSING",
      sourceType: input.sourceType,
      sourceId: input.sourceId,
      createdAt: now(),
      completedAt: null,
    };
    intent.refundedAmount += input.amount;
    intent.status = intent.refundedAmount >= intent.capturedAmount ? "REFUNDED" : "PARTIALLY_REFUNDED";
    intent.transactions.push({
      id: seq("TX"), intentId: intent.id, attemptId: null,
      kind: "REFUND", amount: input.amount, gatewayRef: `GW-${seq("R")}`, at: now(),
    });
    intent.updatedAt = now();
    REFUNDS[intent.id] = [...(REFUNDS[intent.id] ?? []), refund];
    return mockSuccess(refund);
  },

  async listRefunds(intentId: string): Promise<ApiResponse<RefundTransaction[]>> {
    await simulateDelay(120);
    return mockSuccess(REFUNDS[intentId] ?? []);
  },
};
