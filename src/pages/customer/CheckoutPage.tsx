import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { ChevronLeft, ChevronRight, AlertCircle } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

import { useAuth } from "@/hooks/useAuth";
import { useProfile } from "@/hooks/useProfile";
import { useCart } from "@/hooks/useCart";
import { useCheckout } from "@/hooks/useCheckout";
import { useNotificationStore } from "@/store/notificationStore";

import { CheckoutStepper } from "@/components/checkout/CheckoutStepper";
import { AddressSelector } from "@/components/checkout/AddressSelector";
import { ShippingMethodSelector } from "@/components/checkout/ShippingMethodSelector";
import { PaymentMethodSelector } from "@/components/checkout/PaymentMethodSelector";
import { OrderReview } from "@/components/checkout/OrderReview";
import { CheckoutSummary } from "@/components/checkout/CheckoutSummary";
import { ReservationTimer } from "@/components/checkout/ReservationTimer";
import { EmptyCart } from "@/components/cart/EmptyCart";

import { SHIPPING_OPTIONS } from "@/lib/pricing";
import type { Address } from "@/data/mock-users";
import type { PaymentMethodId } from "@/types/checkout";

export default function CheckoutPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const { user } = useAuth();
  const { addresses, addAddress, defaultAddress } = useProfile();
  const { items, vendorGroups, isEmpty, clear } = useCart();
  const {
    session, status, error,
    pricing, init, setAddress, setPaymentMethod, setVendorShipping,
    next, prev, goToStep, reserveInventory, releaseReservation,
    refreshReservationIfExpired, buildOrderDraft, placeOrder, reset,
  } = useCheckout();

  const [placing, setPlacing] = useState(false);

  useEffect(() => {
    if (!user?.id || isEmpty) return;
    if (!session || session.ownerId !== user.id) void init(user.id, items);
  }, [user?.id, isEmpty, items, session, init]);

  useEffect(() => {
    if (session && !session.addressId && defaultAddress) void setAddress(defaultAddress.id);
  }, [session, defaultAddress, setAddress]);

  useEffect(() => {
    if (!session) return;
    if (Object.keys(session.shippingByVendor).length > 0) return;
    const std = SHIPPING_OPTIONS[0];
    vendorGroups.forEach(g => setVendorShipping(g.vendorId, {
      vendorId: g.vendorId, methodId: std.id, cost: std.cost, estimatedDays: std.estimatedDays,
    }));
  }, [session, vendorGroups, setVendorShipping]);

  useEffect(() => {
    if (session?.step === "review" && !session.reservation) void reserveInventory(items);
  }, [session?.step, session?.reservation, items, reserveInventory]);

  const selectedAddress: Address | null = useMemo(
    () => addresses.find(a => a.id === session?.addressId) ?? null,
    [addresses, session?.addressId]
  );

  if (isEmpty && !placing) {
    return <div className="container py-10"><EmptyCart /></div>;
  }

  async function handlePlaceOrder() {
    if (!session || !selectedAddress) return;
    if (refreshReservationIfExpired()) {
      toast({ title: "Reservation expired", description: "We'll reserve your items again.", variant: "destructive" });
      await reserveInventory(items);
      return;
    }
    const draft = buildOrderDraft(selectedAddress);
    if (!draft) {
      toast({ title: "Cannot place order", description: "Missing payment or address.", variant: "destructive" });
      return;
    }
    setPlacing(true);
    const result = await placeOrder(draft);
    if (result) {
      useNotificationStore.getState().addNotification({
        type: "order",
        title: "Order placed",
        message: `Your order ${result.orderId} has been placed successfully.`,
      });
      clear();
      await releaseReservation();
      reset();
      navigate(`/checkout/success?orderId=${result.orderId}`, { replace: true });
    } else {
      setPlacing(false);
      navigate(`/checkout/failure?reason=${encodeURIComponent(error ?? "unknown")}`, { replace: true });
    }
  }

  const step = session?.step ?? "address";
  const canAdvance = step === "address" ? !!session?.addressId
                   : step === "shipping" ? Object.keys(session?.shippingByVendor ?? {}).length === vendorGroups.length
                   : step === "payment"  ? !!session?.payment
                   : true;

  return (
    <div className="container py-6 max-w-6xl">
      <h1 className="font-display text-2xl font-bold mb-4">Checkout</h1>
      <div className="mb-6"><CheckoutStepper current={step} onJump={goToStep} /></div>

      {error && (
        <Alert variant="destructive" className="mb-4">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {step === "review" && (
        <div className="mb-4">
          <ReservationTimer reservation={session?.reservation ?? null} onExpire={() => void reserveInventory(items)} />
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          {step === "address" && (
            <Card>
              <CardHeader><CardTitle className="text-base">Delivery address</CardTitle></CardHeader>
              <CardContent>
                <AddressSelector
                  addresses={addresses}
                  selectedId={session?.addressId ?? null}
                  onSelect={(id) => void setAddress(id)}
                  onAdd={(addr) => void addAddress(addr)}
                />
              </CardContent>
            </Card>
          )}

          {step === "shipping" && (
            <Card>
              <CardHeader><CardTitle className="text-base">Shipping method</CardTitle></CardHeader>
              <CardContent>
                <ShippingMethodSelector
                  vendorGroups={vendorGroups}
                  selection={session?.shippingByVendor ?? {}}
                  onChange={setVendorShipping}
                />
              </CardContent>
            </Card>
          )}

          {step === "payment" && (
            <Card>
              <CardHeader><CardTitle className="text-base">Payment method</CardTitle></CardHeader>
              <CardContent>
                <PaymentMethodSelector
                  selected={session?.payment?.methodId ?? null}
                  onSelect={(id: PaymentMethodId) => setPaymentMethod({ methodId: id })}
                />
              </CardContent>
            </Card>
          )}

          {step === "review" && selectedAddress && session?.payment && (
            <OrderReview
              items={items}
              address={selectedAddress}
              shipping={session.shippingByVendor}
              payment={session.payment}
            />
          )}

          <div className="flex items-center justify-between pt-2">
            <Button variant="outline" size="sm" onClick={prev} disabled={step === "address" || placing}>
              <ChevronLeft className="h-4 w-4 mr-1" /> Back
            </Button>
            {step !== "review" && (
              <Button size="sm" onClick={next} disabled={!canAdvance || status === "saving"}>
                Continue <ChevronRight className="h-4 w-4 ml-1" />
              </Button>
            )}
          </div>
        </div>

        <div className="lg:col-span-1">
          <CheckoutSummary
            pricing={pricing}
            showCoupon={step !== "review"}
            primaryLabel={step === "review" ? "Place Order" : "Continue"}
            primaryDisabled={step === "review" ? (!selectedAddress || !session?.payment) : !canAdvance}
            primaryLoading={placing || status === "placing"}
            onPrimary={step === "review" ? handlePlaceOrder : next}
          />
        </div>
      </div>
    </div>
  );
}