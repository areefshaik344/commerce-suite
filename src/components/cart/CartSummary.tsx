import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Button } from "@/components/ui/button";
import { ArrowRight } from "lucide-react";
import { formatPrice } from "@/lib/pricing";
import { CouponInput } from "./CouponInput";
import type { PricingBreakdown } from "@/types/checkout";

interface Props {
  pricing: PricingBreakdown;
  itemCount: number;
  onCheckout: () => void;
  checkoutDisabled?: boolean;
}

export function CartSummary({ pricing, itemCount, onCheckout, checkoutDisabled }: Props) {
  return (
    <Card className="shadow-card sticky top-24">
      <CardContent className="p-4 space-y-3">
        <h3 className="font-display font-semibold">Order Summary</h3>
        <Separator />

        {pricing.vendorBreakdowns.length > 1 && pricing.vendorBreakdowns.map(v => (
          <div key={v.vendorId} className="flex justify-between text-xs">
            <span className="text-muted-foreground truncate pr-2">{v.vendorName} · {v.itemCount} item{v.itemCount !== 1 ? "s" : ""}</span>
            <span>{formatPrice(v.subtotal)}</span>
          </div>
        ))}

        <Separator />

        <CouponInput baseAmount={pricing.subtotal} />

        <Separator />

        <div className="space-y-2 text-sm">
          <Row label={`Subtotal (${itemCount} item${itemCount !== 1 ? "s" : ""})`} value={formatPrice(pricing.subtotal)} />
          {pricing.discount > 0 && (
            <Row label="Discount" value={`−${formatPrice(pricing.discount)}`} valueClass="text-success" />
          )}
          <Row label="Shipping" value={pricing.shipping === 0 ? "Free" : formatPrice(pricing.shipping)}
               valueClass={pricing.shipping === 0 ? "text-success" : undefined} />
          <Row label="Tax (GST 18%)" value={formatPrice(pricing.tax)} />
          {pricing.platformFee > 0 && <Row label="Fees" value={formatPrice(pricing.platformFee)} />}
        </div>
        <Separator />
        <div className="flex justify-between font-display font-bold text-lg">
          <span>Total</span>
          <span>{formatPrice(pricing.grandTotal)}</span>
        </div>
        <Button className="w-full gap-2" size="lg" disabled={checkoutDisabled} onClick={onCheckout}>
          Proceed to Checkout <ArrowRight className="h-4 w-4" />
        </Button>
      </CardContent>
    </Card>
  );
}

function Row({ label, value, valueClass }: { label: string; value: string; valueClass?: string }) {
  return (
    <div className="flex justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span className={valueClass}>{value}</span>
    </div>
  );
}