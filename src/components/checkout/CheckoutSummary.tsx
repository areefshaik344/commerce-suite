import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { PriceBreakdown } from "./PriceBreakdown";
import { CouponInput } from "@/components/cart/CouponInput";
import type { PricingBreakdown } from "@/types/checkout";
import { Loader2 } from "lucide-react";

interface Props {
  pricing: PricingBreakdown;
  primaryLabel: string;
  onPrimary: () => void;
  primaryDisabled?: boolean;
  primaryLoading?: boolean;
  showCoupon?: boolean;
}

export function CheckoutSummary({ pricing, primaryLabel, onPrimary, primaryDisabled, primaryLoading, showCoupon }: Props) {
  return (
    <Card className="shadow-card sticky top-24">
      <CardContent className="p-4 space-y-3">
        <h3 className="font-display font-semibold">Order Summary</h3>
        {showCoupon && <CouponInput baseAmount={pricing.subtotal} />}
        <PriceBreakdown pricing={pricing} />
        <Button className="w-full" size="lg" onClick={onPrimary} disabled={primaryDisabled || primaryLoading}>
          {primaryLoading && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
          {primaryLabel}
        </Button>
      </CardContent>
    </Card>
  );
}