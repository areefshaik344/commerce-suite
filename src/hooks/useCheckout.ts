import { useEffect, useMemo } from "react";
import { useCheckoutStore } from "@/store/checkoutStore";
import { useCartStore } from "@/store/cartStore";
import { useCouponStore } from "@/store/couponStore";
import { useAuthStore } from "@/store/authStore";
import { computePricing, PRICING_CONFIG, SHIPPING_OPTIONS } from "@/lib/pricing";
import { checkoutApi } from "@/api/checkoutApi";
import type { PaymentSelection, VendorShippingSelection } from "@/types/checkout";
import type { Address } from "@/data/mock-users";

export function useCheckout(opts: { autoInit?: boolean } = {}) {
  const user = useAuthStore(s => s.currentUser);
  const items = useCartStore(s => s.items);
  const coupons = useCouponStore(s => s.applied);

  const session = useCheckoutStore(s => s.session);
  const status = useCheckoutStore(s => s.status);
  const error = useCheckoutStore(s => s.error);

  useEffect(() => {
    if (!opts.autoInit) return;
    if (!user?.id || items.length === 0) return;
    if (!session || session.ownerId !== user.id) {
      void useCheckoutStore.getState().init(user.id, items);
    }
  }, [opts.autoInit, user?.id, items, session]);

  const paymentSurcharge = useMemo(() => {
    if (!session?.payment) return 0;
    return session.payment.methodId === "cod" ? PRICING_CONFIG.codSurcharge : 0;
  }, [session?.payment]);

  const pricing = useMemo(
    () => computePricing({
      items,
      shipping: session?.shippingByVendor ?? {},
      coupons,
      paymentSurcharge,
    }),
    [items, session?.shippingByVendor, coupons, paymentSurcharge]
  );

  const buildOrderDraft = (address: Address) => {
    if (!user || !session || !session.payment) return null;
    return checkoutApi.buildOrderDraft({
      ownerId: user.id,
      address,
      items,
      shipping: session.shippingByVendor,
      payment: session.payment,
      reservationId: session.reservation?.id ?? null,
      pricing,
    });
  };

  const canAdvance = (): boolean => {
    if (!session) return false;
    switch (session.step) {
      case "address":  return !!session.addressId;
      case "shipping": return Object.keys(session.shippingByVendor).length > 0;
      case "payment":  return !!session.payment;
      case "review":   return true;
    }
  };

  return {
    session, status, error,
    pricing, items,
    shippingOptions: SHIPPING_OPTIONS,
    init: useCheckoutStore.getState().init,
    setAddress: useCheckoutStore.getState().setAddress,
    setShipping: useCheckoutStore.getState().setShipping,
    setPayment: useCheckoutStore.getState().setPayment,
    next: useCheckoutStore.getState().next,
    prev: useCheckoutStore.getState().prev,
    goToStep: useCheckoutStore.getState().goToStep,
    reserveInventory: useCheckoutStore.getState().reserveInventory,
    refreshReservationIfExpired: useCheckoutStore.getState().refreshReservationIfExpired,
    releaseReservation: useCheckoutStore.getState().releaseReservation,
    setPricingSnapshot: useCheckoutStore.getState().setPricingSnapshot,
    placeOrder: useCheckoutStore.getState().placeOrder,
    reset: useCheckoutStore.getState().reset,
    buildOrderDraft,
    canAdvance,
    setVendorShipping(vendorId: string, sel: VendorShippingSelection) {
      const next = { ...(session?.shippingByVendor ?? {}), [vendorId]: sel };
      void useCheckoutStore.getState().setShipping(next);
    },
    setPaymentMethod(payment: PaymentSelection) {
      void useCheckoutStore.getState().setPayment(payment);
    },
  };
}