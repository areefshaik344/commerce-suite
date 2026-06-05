import { useEffect, useMemo } from "react";
import { useInvoiceStore } from "@/store/invoiceStore";

export function useInvoicesForOrder(orderId: string | undefined) {
  const ids = useInvoiceStore(s => orderId ? s.byOrder[orderId] : undefined);
  const byId = useInvoiceStore(s => s.byId);
  const loading = useInvoiceStore(s => s.loading);
  const fetchForOrder = useInvoiceStore(s => s.fetchForOrder);
  const download = useInvoiceStore(s => s.download);

  useEffect(() => {
    if (!orderId) return;
    void fetchForOrder(orderId);
  }, [orderId]);

  const invoices = useMemo(() => (ids ?? []).map(id => byId[id]).filter(Boolean), [ids, byId]);
  return { invoices, loading, download };
}
