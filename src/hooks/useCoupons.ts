import { useCouponStore } from "@/store/couponStore";

export function useCoupons() {
  const applied = useCouponStore(s => s.applied);
  const applyingCode = useCouponStore(s => s.applyingCode);
  const error = useCouponStore(s => s.error);
  return {
    applied, applyingCode, error,
    apply: useCouponStore.getState().apply,
    remove: useCouponStore.getState().remove,
    clear: useCouponStore.getState().clear,
  };
}