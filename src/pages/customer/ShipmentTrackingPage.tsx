import { useParams, Link } from "react-router-dom";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { ArrowLeft, PackageX } from "lucide-react";
import { useShipment } from "@/hooks/useShipments";
import { ShipmentStatusBadge } from "@/components/shipping/ShipmentStatusBadge";
import { DeliveryEstimateCard } from "@/components/shipping/DeliveryEstimateCard";
import { TrackingTimeline } from "@/components/shipping/TrackingTimeline";

export default function ShipmentTrackingPage() {
  const { id } = useParams<{ id: string }>();
  const { detail, loading } = useShipment(id);

  return (
    <div className="container py-6 max-w-3xl">
      <Button variant="ghost" size="sm" asChild className="mb-4">
        <Link to="/orders"><ArrowLeft className="h-4 w-4 mr-1" /> Back to orders</Link>
      </Button>

      {loading && !detail ? (
        <div className="space-y-3">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-48 w-full" />
        </div>
      ) : !detail ? (
        <div className="text-center py-16">
          <PackageX className="h-12 w-12 mx-auto text-muted-foreground/40 mb-3" />
          <p className="text-muted-foreground">Shipment not found.</p>
        </div>
      ) : (
        <div className="space-y-4">
          <header className="flex items-start justify-between">
            <div>
              <h1 className="font-display text-xl font-bold">Tracking</h1>
              <p className="text-sm text-muted-foreground">
                Shipment <span className="font-mono">{detail.shipment.id}</span>
                {detail.shipment.trackingNumber && <> · {detail.shipment.carrier ?? "Courier"} <span className="font-mono">{detail.shipment.trackingNumber}</span></>}
              </p>
            </div>
            <ShipmentStatusBadge status={detail.shipment.status} />
          </header>

          <DeliveryEstimateCard estimate={detail.estimate} />

          <section>
            <h2 className="text-sm font-semibold mb-3">Activity</h2>
            <TrackingTimeline events={detail.events} />
          </section>
        </div>
      )}
    </div>
  );
}
