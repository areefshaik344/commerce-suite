import { simulateDelay, mockSuccess, ApiError, type ApiResponse } from "./apiClient";
import type {
  ShipmentDetail, ShipmentPackage, ShipmentLeg, TrackingEvent,
  DeliveryEstimate, ShippingLabel, FulfillmentTask, CourierProvider,
  FulfillmentStatus,
} from "@/types/shipping";
import { toShipmentTimelineEvent } from "@/types/shipping";
import type { OrderRecord, Shipment } from "@/types/order";
import { shipmentApi, __bindShipmentDataset } from "./shipmentApi";
import { mockOrderRecords } from "@/mocks/mockOrderRecords";

let ORDERS: OrderRecord[] = mockOrderRecords;
__bindShipmentDataset(ORDERS);

const COURIERS: CourierProvider[] = [
  { id: "courier-delhivery", name: "Delhivery",  slug: "delhivery", supportsCod: true,  supportsReverse: true,  active: true, trackingUrlTemplate: "https://delhivery.com/track/{tracking}" },
  { id: "courier-bluedart",  name: "Blue Dart",  slug: "bluedart",  supportsCod: false, supportsReverse: false, active: true, trackingUrlTemplate: "https://bluedart.com/track/{tracking}" },
  { id: "courier-ecom",      name: "Ecom Express", slug: "ecom",    supportsCod: true,  supportsReverse: true,  active: true },
];

const PACKAGES: Record<string, ShipmentPackage[]> = {};
const LEGS: Record<string, ShipmentLeg[]> = {};
const EVENTS: Record<string, TrackingEvent[]> = {};
const ESTIMATES: Record<string, DeliveryEstimate> = {};
const LABELS: Record<string, ShippingLabel[]> = {};
const TASKS: Record<string, FulfillmentTask[]> = {};

const seq = (p: string) => `${p}-${Date.now().toString(36)}-${Math.floor(Math.random() * 1e4).toString(36)}`;

function hydrate(shipment: Shipment): ShipmentDetail {
  return {
    shipment,
    packages: PACKAGES[shipment.id] ?? [],
    legs: LEGS[shipment.id] ?? [],
    events: EVENTS[shipment.id] ?? shipment.timeline.map(t => ({
      id: t.id, shipmentId: shipment.id, legId: null,
      type: "DEPARTED_HUB", status: "IN_TRANSIT", at: t.at,
      location: t.location, note: t.note,
    })),
    estimate: ESTIMATES[shipment.id] ?? (shipment.estimatedDeliveryAt ? {
      shipmentId: shipment.id,
      earliestAt: shipment.estimatedDeliveryAt,
      latestAt: shipment.estimatedDeliveryAt,
      confidence: 0.75,
      generatedAt: shipment.createdAt,
      source: "rule",
    } : null),
    labels: LABELS[shipment.id] ?? [],
    tasks: TASKS[shipment.id] ?? [],
  };
}

function findShipment(id: string): { order: OrderRecord; shipment: Shipment } | null {
  for (const o of ORDERS) {
    const s = o.shipments.find(x => x.id === id);
    if (s) return { order: o, shipment: s };
  }
  return null;
}

export const shippingApi = {
  async listCouriers(): Promise<ApiResponse<CourierProvider[]>> {
    await simulateDelay(120);
    return mockSuccess(COURIERS.filter(c => c.active));
  },

  async getShipmentDetail(shipmentId: string): Promise<ApiResponse<ShipmentDetail>> {
    await simulateDelay(180);
    const found = findShipment(shipmentId);
    if (!found) throw new ApiError("Shipment not found", 404, "SHIPMENT_NOT_FOUND");
    return mockSuccess(hydrate(found.shipment));
  },

  async trackByNumber(trackingNumber: string): Promise<ApiResponse<ShipmentDetail>> {
    await simulateDelay(220);
    for (const o of ORDERS) {
      const s = o.shipments.find(x => x.trackingNumber === trackingNumber);
      if (s) return mockSuccess(hydrate(s));
    }
    throw new ApiError("Tracking number not found", 404, "TRACKING_NOT_FOUND");
  },

  async listForOrder(orderId: string): Promise<ApiResponse<ShipmentDetail[]>> {
    await simulateDelay(160);
    const order = ORDERS.find(o => o.id === orderId);
    if (!order) throw new ApiError("Order not found", 404, "ORDER_NOT_FOUND");
    return mockSuccess(order.shipments.map(hydrate));
  },

  async listForVendor(vendorId: string): Promise<ApiResponse<ShipmentDetail[]>> {
    await simulateDelay(180);
    const out: ShipmentDetail[] = [];
    for (const o of ORDERS) for (const s of o.shipments) if (s.vendorId === vendorId) out.push(hydrate(s));
    return mockSuccess(out);
  },

  async estimateDelivery(input: { pincode: string; vendorId: string }): Promise<ApiResponse<{ earliestDays: number; latestDays: number; confidence: number }>> {
    await simulateDelay(150);
    const hash = [...input.pincode].reduce((a, c) => a + c.charCodeAt(0), 0);
    const earliest = 2 + (hash % 3);
    return mockSuccess({ earliestDays: earliest, latestDays: earliest + 2, confidence: 0.8 });
  },

  async generateLabel(input: { shipmentId: string; courierId: string; actorId: string }): Promise<ApiResponse<ShippingLabel>> {
    await simulateDelay(250);
    const found = findShipment(input.shipmentId);
    if (!found) throw new ApiError("Shipment not found", 404, "SHIPMENT_NOT_FOUND");
    const label: ShippingLabel = {
      id: seq("LBL"),
      shipmentId: input.shipmentId,
      courierId: input.courierId,
      trackingNumber: `TRK${Math.floor(Math.random() * 1e10).toString().padStart(10, "0")}`,
      pdfUrl: null,
      createdAt: new Date().toISOString(),
      voidedAt: null,
    };
    LABELS[input.shipmentId] = [...(LABELS[input.shipmentId] ?? []), label];
    return mockSuccess(label);
  },

  async recordTrackingEvent(input: {
    shipmentId: string; type: TrackingEvent["type"]; status: FulfillmentStatus;
    location?: string; note?: string; actorId: string; actorRole: "vendor" | "admin";
  }): Promise<ApiResponse<{ event: TrackingEvent; shipment: Shipment }>> {
    await simulateDelay(200);
    const found = findShipment(input.shipmentId);
    if (!found) throw new ApiError("Shipment not found", 404, "SHIPMENT_NOT_FOUND");
    const ev: TrackingEvent = {
      id: seq("TEV"),
      shipmentId: input.shipmentId,
      legId: null,
      type: input.type,
      status: input.status,
      location: input.location,
      note: input.note,
      at: new Date().toISOString(),
    };
    EVENTS[input.shipmentId] = [...(EVENTS[input.shipmentId] ?? []), ev];
    // Mirror to canonical shipment timeline via the order-side API for status transitions.
    const res = await shipmentApi.updateStatus({
      orderId: found.order.id,
      shipmentId: input.shipmentId,
      status: toShipmentTimelineEvent(ev).status,
      location: ev.location,
      note: ev.note,
      actorId: input.actorId,
      actorRole: input.actorRole,
    }).catch(() => null);
    return mockSuccess({ event: ev, shipment: res?.data.shipment ?? found.shipment });
  },

  async createPackage(input: Omit<ShipmentPackage, "id" | "createdAt">): Promise<ApiResponse<ShipmentPackage>> {
    await simulateDelay(160);
    const pkg: ShipmentPackage = { ...input, id: seq("PKG"), createdAt: new Date().toISOString() };
    PACKAGES[input.shipmentId] = [...(PACKAGES[input.shipmentId] ?? []), pkg];
    return mockSuccess(pkg);
  },

  async listTasksForVendor(vendorId: string): Promise<ApiResponse<FulfillmentTask[]>> {
    await simulateDelay(140);
    const out: FulfillmentTask[] = [];
    for (const list of Object.values(TASKS)) for (const t of list) if (t.vendorId === vendorId) out.push(t);
    return mockSuccess(out);
  },
};
