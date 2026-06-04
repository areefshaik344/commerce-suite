import { useMemo } from "react";
import { useOrder } from "./useOrders";
import type { Shipment } from "@/types/order";

export function useOrderTracking(orderId: string | undefined) {
  const { order, loading } = useOrder(orderId);
  const shipments: Shipment[] = useMemo(() => order?.shipments ?? [], [order]);
  const nextEta = useMemo(() => {
    const dates = shipments
      .map(s => s.estimatedDeliveryAt)
      .filter((d): d is string => !!d)
      .map(d => new Date(d).getTime())
      .sort((a, b) => a - b);
    return dates[0] ? new Date(dates[0]).toISOString() : null;
  }, [shipments]);
  return { order, shipments, nextEta, loading };
}