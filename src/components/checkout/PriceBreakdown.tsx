import { Separator } from "@/components/ui/separator";
import { formatPrice } from "@/lib/pricing";
import type { PricingBreakdown } from "@/types/checkout";

export function PriceBreakdown({ pricing, compact }: { pricing: PricingBreakdown; compact?: boolean }) {
  return (
    <div className={compact ? "space-y-1.5 text-sm" : "space-y-2 text-sm"}>
      <Row label="Subtotal" value={formatPrice(pricing.subtotal)} />
      {pricing.discount > 0 && <Row label="Discount" value={`−${formatPrice(pricing.discount)}`} valueClass="text-success" />}
      <Row label="Shipping" value={pricing.shipping === 0 ? "Free" : formatPrice(pricing.shipping)}
           valueClass={pricing.shipping === 0 ? "text-success" : undefined} />
      <Row label="Tax (GST 18%)" value={formatPrice(pricing.tax)} />
      {pricing.platformFee > 0 && <Row label="Fees & charges" value={formatPrice(pricing.platformFee)} />}
      <Separator />
      <div className="flex justify-between font-display font-bold text-lg">
        <span>Total</span>
        <span>{formatPrice(pricing.grandTotal)}</span>
      </div>
    </div>
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