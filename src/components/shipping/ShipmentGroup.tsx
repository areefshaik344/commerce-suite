import { memo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ShipmentStatusBadge } from "./ShipmentStatusBadge";
import { DeliveryEstimateCard } from "./DeliveryEstimateCard";
import { TrackingTimeline } from "./TrackingTimeline";
import { Button } from "@/components/ui/button";
import { ExternalLink } from "lucide-react";
import { Link } from "react-router-dom";
import type { ShipmentDetail } from "@/types/shipping";

interface Props { detail: ShipmentDetail }

function ShipmentGroupImpl({ detail }: Props) {
  const { shipment, events, estimate } = detail;
  return (
    <Card className="shadow-card">
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <div className="space-y-1">
          <CardTitle className="text-base">Shipment {shipment.id}</CardTitle>
          {shipment.trackingNumber && (
            <p className="text-xs text-muted-foreground">
              Tracking: <span className="font-mono">{shipment.trackingNumber}</span>
              {shipment.carrier && <> · {shipment.carrier}</>}
            </p>
          )}
        </div>
        <ShipmentStatusBadge status={shipment.status} />
      </CardHeader>
      <CardContent className="space-y-4">
        <DeliveryEstimateCard estimate={estimate} />
        <TrackingTimeline events={events} />
        <div className="flex justify-end">
          <Button size="sm" variant="outline" asChild>
            <Link to={`/tracking/${shipment.id}`}>
              View full tracking <ExternalLink className="h-3.5 w-3.5 ml-1" />
            </Link>
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

export const ShipmentGroup = memo(ShipmentGroupImpl);
