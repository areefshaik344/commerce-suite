import { useEffect } from "react";
import { usePaymentStore } from "@/store/paymentStore";

export function usePaymentMethods() {
  const methods = usePaymentStore(s => s.methods);
  const loading = usePaymentStore(s => s.loading);
  const loadMethods = usePaymentStore(s => s.loadMethods);
  useEffect(() => {
    if (methods.length === 0) void loadMethods();
  }, [methods.length]);
  return { methods, loading };
}

export function usePaymentIntent(intentId: string | undefined) {
  const intent = usePaymentStore(s => intentId ? s.intentsById[intentId] : undefined);
  const getIntent = usePaymentStore(s => s.getIntent);
  const confirming = usePaymentStore(s => s.confirming);
  const confirm = usePaymentStore(s => s.confirm);
  const retry = usePaymentStore(s => s.retry);
  const cancel = usePaymentStore(s => s.cancel);

  useEffect(() => {
    if (!intentId) return;
    void getIntent(intentId);
  }, [intentId]);

  return { intent, confirming, confirm, retry, cancel };
}

export function useRefunds(intentId: string | undefined) {
  const refunds = usePaymentStore(s => intentId ? s.refundsByIntent[intentId] ?? [] : []);
  const loadRefunds = usePaymentStore(s => s.loadRefunds);
  const refund = usePaymentStore(s => s.refund);
  useEffect(() => {
    if (!intentId) return;
    void loadRefunds(intentId);
  }, [intentId]);
  return { refunds, refund };
}
