import { create } from "zustand";
import { persist } from "zustand/middleware";
import { couponApi } from "@/api/couponApi";
import { ApiError } from "@/api/apiClient";
import type { AppliedCoupon } from "@/types/checkout";

interface CouponState {
  applied: AppliedCoupon[];
  applyingCode: string | null;
  error: string | null;
  apply: (code: string, baseAmount: number, vendorId?: string) => Promise<AppliedCoupon | null>;
  remove: (code: string) => void;
  clear: () => void;
  reset: () => void;
}

export const useCouponStore = create<CouponState>()(
  persist(
    (set, get) => ({
      applied: [],
      applyingCode: null,
      error: null,
      apply: async (code, baseAmount, vendorId) => {
        const normalized = code.trim().toUpperCase();
        if (!normalized) return null;
        if (get().applied.some(a => a.code === normalized)) {
          set({ error: "Coupon already applied" });
          return null;
        }
        const placeholder: AppliedCoupon = {
          code: normalized, label: normalized, discount: 0,
          type: "FLAT", scope: "GLOBAL", appliedAt: Date.now(),
        };
        set({ applyingCode: normalized, error: null, applied: [...get().applied, placeholder] });
        try {
          const res = await couponApi.validateCoupon({ code: normalized, baseAmount, vendorId });
          set({
            applyingCode: null,
            applied: get().applied.map(a => a.code === normalized ? res.data.applied : a),
          });
          return res.data.applied;
        } catch (e) {
          const msg = e instanceof ApiError ? e.message : "Invalid coupon";
          set({
            applyingCode: null,
            error: msg,
            applied: get().applied.filter(a => a.code !== normalized),
          });
          return null;
        }
      },
      remove: (code) => set({ applied: get().applied.filter(a => a.code !== code), error: null }),
      clear: () => set({ applied: [], error: null }),
      reset: () => set({ applied: [], applyingCode: null, error: null }),
    }),
    { name: "markethub-coupons", partialize: (s) => ({ applied: s.applied }) }
  )
);