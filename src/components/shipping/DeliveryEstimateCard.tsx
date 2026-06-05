import { Card, CardContent } from "@/components/ui/card";
import { Truck } from "lucide-react";
import type { DeliveryEstimate } from "@/types/shipping";

interface Props { estimate: DeliveryEstimate | null }

function fmt(d: string) {
  return new Date(d).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

export function DeliveryEstimateCard({ estimate }: Props) {
  if (!estimate) return null;
  return (
    <Card className="shadow-card">
      <CardContent className="flex items-start gap-3 py-4">
        <div className="p-2 bg-primary/10 text-primary rounded-full"><Truck className="h-5 w-5" /></div>
        <div className="flex-1">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Estimated delivery</p>
          <p className="font-semibold text-sm">
            {fmt(estimate.earliestAt)} – {fmt(estimate.latestAt)}
          </p>
          <p className="text-xs text-muted-foreground mt-0.5">
            Confidence {Math.round(estimate.confidence * 100)}%
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
