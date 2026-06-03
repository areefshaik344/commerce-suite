import { create } from "zustand";
import { persist } from "zustand/middleware";
import { checkoutApi, RESERVATION_TTL_MS } from "@/api/checkoutApi";
import { ApiError } from "@/api/apiClient";
import {
  type CheckoutSession, type CheckoutStep, type PaymentSelection,
  type VendorShippingSelection, type ReservationDto, type OrderDraft,
  type CartItem, type PricingBreakdown,
  isReservationExpired,
} from "@/types/checkout";

type Status = "idle" | "loading" | "saving" | "placing" | "error";

interface CheckoutState {
  session: CheckoutSession | null;
  status: Status;
  error: string | null;
  init: (ownerId: string, items: CartItem[]) => Promise<CheckoutSession | null>;
  setAddress: (addressId: string) => Promise<void>;
  setShipping: (selection: Record<string, VendorShippingSelection>) => Promise<void>;
  setPayment: (payment: PaymentSelection) => Promise<void>;
  goToStep: (step: CheckoutStep) => void;
  next: () => void;
  prev: () => void;
  reserveInventory: (items: CartItem[]) => Promise<ReservationDto | null>;
  refreshReservationIfExpired: () => boolean;
  releaseReservation: () => Promise<void>;
  setPricingSnapshot: (p: PricingBreakdown) => void;
  placeOrder: (draft: OrderDraft) => Promise<{ orderId: string } | null>;
  reset: () => void;
}

const ORDER: CheckoutStep[] = ["address", "shipping", "payment", "review"];

function canAdvance(s: CheckoutSession): boolean {
  switch (s.step) {
    case "address":  return !!s.addressId;
    case "shipping": return Object.keys(s.shippingByVendor).length > 0;
    case "payment":  return !!s.payment;
    case "review":   return false;
  }
}

export const useCheckoutStore = create<CheckoutState>()(
  persist(
    (set, get) => ({
      session: null,
      status: "idle",
      error: null,
      init: async (ownerId, items) => {
        const existing = get().session;
        if (existing && existing.ownerId === ownerId && !isReservationExpired(existing.reservation)) {
          return existing;
        }
        set({ status: "loading", error: null });
        try {
          const res = await checkoutApi.createSession({ ownerId, items });
          set({ session: res.data, status: "idle" });
          return res.data;
        } catch (e) {
          set({ status: "error", error: e instanceof ApiError ? e.message : "Could not start checkout" });
          return null;
        }
      },
      setAddress: async (addressId) => {
        const s = get().session; if (!s) return;
        set({ status: "saving" });
        try {
          await checkoutApi.setAddress(s.id, addressId);
          set({ session: { ...s, addressId, updatedAt: Date.now() }, status: "idle" });
        } catch (e) { set({ status: "error", error: e instanceof ApiError ? e.message : "Could not save address" }); }
      },
      setShipping: async (selection) => {
        const s = get().session; if (!s) return;
        set({ status: "saving" });
        try {
          await checkoutApi.setShipping(s.id, selection);
          set({ session: { ...s, shippingByVendor: selection, updatedAt: Date.now() }, status: "idle" });
        } catch (e) { set({ status: "error", error: e instanceof ApiError ? e.message : "Could not save shipping" }); }
      },
      setPayment: async (payment) => {
        const s = get().session; if (!s) return;
        set({ status: "saving" });
        try {
          await checkoutApi.setPayment(s.id, payment);
          set({ session: { ...s, payment, updatedAt: Date.now() }, status: "idle" });
        } catch (e) { set({ status: "error", error: e instanceof ApiError ? e.message : "Could not save payment" }); }
      },
      goToStep: (step) => {
        const s = get().session; if (!s) return;
        set({ session: { ...s, step, updatedAt: Date.now() } });
      },
      next: () => {
        const s = get().session; if (!s) return;
        if (!canAdvance(s)) return;
        const idx = ORDER.indexOf(s.step);
        set({ session: { ...s, step: ORDER[Math.min(idx + 1, ORDER.length - 1)], updatedAt: Date.now() } });
      },
      prev: () => {
        const s = get().session; if (!s) return;
        const idx = ORDER.indexOf(s.step);
        set({ session: { ...s, step: ORDER[Math.max(idx - 1, 0)], updatedAt: Date.now() } });
      },
      reserveInventory: async (items) => {
        const s = get().session; if (!s) return null;
        try {
          const res = await checkoutApi.reserveInventory(s.ownerId, items);
          set({ session: { ...s, reservation: res.data, updatedAt: Date.now() }, error: null });
          return res.data;
        } catch (e) {
          set({ error: e instanceof ApiError ? e.message : "Could not reserve inventory" });
          return null;
        }
      },
      refreshReservationIfExpired: () => {
        const s = get().session; if (!s) return false;
        if (isReservationExpired(s.reservation)) {
          set({ session: { ...s, reservation: null, updatedAt: Date.now() } });
          return true;
        }
        return false;
      },
      releaseReservation: async () => {
        const s = get().session; if (!s?.reservation) return;
        try { await checkoutApi.releaseReservation(s.reservation.id); } catch { /* ignore */ }
        set({ session: { ...s, reservation: null, updatedAt: Date.now() } });
      },
      setPricingSnapshot: (p) => {
        const s = get().session; if (!s) return;
        set({ session: { ...s, pricingSnapshot: p, updatedAt: Date.now() } });
      },
      placeOrder: async (draft) => {
        set({ status: "placing", error: null });
        try {
          const res = await checkoutApi.placeOrder({ draft });
          set({ status: "idle" });
          return { orderId: res.data.orderId };
        } catch (e) {
          set({ status: "error", error: e instanceof ApiError ? e.message : "Order placement failed" });
          return null;
        }
      },
      reset: () => set({ session: null, status: "idle", error: null }),
    }),
    { name: "markethub-checkout", partialize: (s) => ({ session: s.session }) }
  )
);

export { RESERVATION_TTL_MS };