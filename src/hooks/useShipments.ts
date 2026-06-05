import { useEffect, useMemo } from "react";
import { useShipmentStore } from "@/store/shipmentStore";
import type { ShipmentDetail } from "@/types/shipping";

export function useShipment(shipmentId: string | undefined) {
  const detail = useShipmentStore(s => shipmentId ? s.detailsById[shipmentId] : undefined);
  const loading = useShipmentStore(s => s.loading);
  const fetchDetail = useShipmentStore(s => s.fetchDetail);

  useEffect(() => {
    if (!shipmentId) return;
    void fetchDetail(shipmentId);
  }, [shipmentId]);

  return { detail, loading };
}

export function useOrderShipments(orderId: string | undefined) {
  const ids = useShipmentStore(s => orderId ? s.shipmentIdsByOrder[orderId] : undefined);
  const detailsById = useShipmentStore(s => s.detailsById);
  const loading = useShipmentStore(s => s.loading);
  const fetchForOrder = useShipmentStore(s => s.fetchForOrder);

  useEffect(() => {
    if (!orderId) return;
    void fetchForOrder(orderId);
  }, [orderId]);

  const details: ShipmentDetail[] = useMemo(
    () => (ids ?? []).map(id => detailsById[id]).filter(Boolean),
    [ids, detailsById],
  );
  return { details, loading };
}

export function useTracking(trackingNumber: string | undefined) {
  const trackByNumber = useShipmentStore(s => s.trackByNumber);
  const detail = useShipmentStore(s => Object.values(s.detailsById).find(d => d.shipment.trackingNumber === trackingNumber));
  const loading = useShipmentStore(s => s.loading);
  useEffect(() => {
    if (!trackingNumber) return;
    void trackByNumber(trackingNumber);
  }, [trackingNumber]);
  return { detail, loading };
}
