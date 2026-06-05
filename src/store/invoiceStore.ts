import { create } from "zustand";
import { invoiceApi } from "@/api/invoiceApi";
import type { Invoice } from "@/types/invoice";

interface InvoiceState {
  byId: Record<string, Invoice>;
  byOrder: Record<string, string[]>;
  loading: boolean;
  error: string | null;
  fetchForOrder: (orderId: string) => Promise<Invoice[]>;
  fetchById: (id: string) => Promise<Invoice | null>;
  download: (id: string) => Promise<string | null>;
}

export const useInvoiceStore = create<InvoiceState>((set) => ({
  byId: {},
  byOrder: {},
  loading: false,
  error: null,

  async fetchForOrder(orderId) {
    set({ loading: true, error: null });
    try {
      const res = await invoiceApi.listForOrder(orderId);
      set(s => ({
        byId: res.data.reduce((acc, i) => ({ ...acc, [i.id]: i }), { ...s.byId }),
        byOrder: { ...s.byOrder, [orderId]: res.data.map(i => i.id) },
        loading: false,
      }));
      return res.data;
    } catch (e) {
      set({ loading: false, error: e instanceof Error ? e.message : "Failed to load invoices" });
      return [];
    }
  },

  async fetchById(id) {
    try {
      const res = await invoiceApi.getById(id);
      set(s => ({ byId: { ...s.byId, [id]: res.data } }));
      return res.data;
    } catch (e) {
      set({ error: e instanceof Error ? e.message : "Failed to load invoice" });
      return null;
    }
  },

  async download(id) {
    try {
      const res = await invoiceApi.download(id);
      return res.data.url;
    } catch {
      return null;
    }
  },
}));
