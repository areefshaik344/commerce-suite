import { create } from "zustand";
import { paymentApi } from "@/api/paymentApi";
import type {
  PaymentIntent, PaymentMethod, RefundTransaction,
} from "@/types/payment";
import type { PaymentMethodId } from "@/types/checkout";

interface PaymentState {
  methods: PaymentMethod[];
  intentsById: Record<string, PaymentIntent>;
  refundsByIntent: Record<string, RefundTransaction[]>;
  loading: boolean;
  confirming: boolean;
  error: string | null;

  loadMethods: () => Promise<PaymentMethod[]>;
  createIntent: (input: { orderId?: string | null; amount: number; methodId: PaymentMethodId; idempotencyKey: string }) => Promise<PaymentIntent>;
  getIntent: (intentId: string) => Promise<PaymentIntent | null>;
  confirm: (intentId: string, simulateOutcome?: "success" | "fail") => Promise<PaymentIntent>;
  retry: (intentId: string) => Promise<PaymentIntent>;
  cancel: (intentId: string) => Promise<PaymentIntent>;
  refund: (input: { intentId: string; amount: number; reason: string; sourceType: RefundTransaction["sourceType"]; sourceId: string; orderId: string }) => Promise<RefundTransaction>;
  loadRefunds: (intentId: string) => Promise<RefundTransaction[]>;
}

export const usePaymentStore = create<PaymentState>((set, get) => ({
  methods: [],
  intentsById: {},
  refundsByIntent: {},
  loading: false,
  confirming: false,
  error: null,

  async loadMethods() {
    set({ loading: true, error: null });
    try {
      const res = await paymentApi.listMethods();
      set({ methods: res.data, loading: false });
      return res.data;
    } catch (e) {
      set({ loading: false, error: e instanceof Error ? e.message : "Failed to load methods" });
      return [];
    }
  },

  async createIntent(input) {
    const res = await paymentApi.createIntent(input);
    set(s => ({ intentsById: { ...s.intentsById, [res.data.id]: res.data } }));
    return res.data;
  },

  async getIntent(intentId) {
    try {
      const res = await paymentApi.getIntent(intentId);
      set(s => ({ intentsById: { ...s.intentsById, [intentId]: res.data } }));
      return res.data;
    } catch (e) {
      set({ error: e instanceof Error ? e.message : "Failed to load intent" });
      return null;
    }
  },

  async confirm(intentId, simulateOutcome) {
    set({ confirming: true, error: null });
    try {
      const res = await paymentApi.confirm({ intentId, simulateOutcome });
      set(s => ({ intentsById: { ...s.intentsById, [intentId]: res.data }, confirming: false }));
      return res.data;
    } catch (e) {
      set({ confirming: false, error: e instanceof Error ? e.message : "Payment failed" });
      throw e;
    }
  },

  async retry(intentId) {
    return get().confirm(intentId, "success");
  },

  async cancel(intentId) {
    const res = await paymentApi.cancel(intentId);
    set(s => ({ intentsById: { ...s.intentsById, [intentId]: res.data } }));
    return res.data;
  },

  async refund(input) {
    const refund = (await paymentApi.refund(input)).data;
    const intent = (await paymentApi.getIntent(input.intentId)).data;
    set(s => ({
      intentsById: { ...s.intentsById, [intent.id]: intent },
      refundsByIntent: { ...s.refundsByIntent, [intent.id]: [...(s.refundsByIntent[intent.id] ?? []), refund] },
    }));
    return refund;
  },

  async loadRefunds(intentId) {
    const res = await paymentApi.listRefunds(intentId);
    set(s => ({ refundsByIntent: { ...s.refundsByIntent, [intentId]: res.data } }));
    return res.data;
  },
}));
