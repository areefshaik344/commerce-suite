/**
 * Shipping domain — backend-ready DTOs.
 *
 * Frozen contracts. UI must NOT mutate these directly. Mock and future
 * backend implementations MUST return these shapes.
 *
 * The Shipment shape lives in `@/types/order` as a snapshot embedded in
 * OrderRecord. This module adds the operational/fulfilment surface:
 * packages, legs, courier providers, labels, estimates and tasks.
 * The order Shipment is the durable record; everything here references
 * it by `shipmentId`.
 */
import type { Shipment, ShipmentStatus, ShipmentTimelineEvent } from "./order";

export const FULFILLMENT_STATUS = {
  CREATED: "CREATED",
  PACKED: "PACKED",
  SHIPPED: "SHIPPED",
  IN_TRANSIT: "IN_TRANSIT",
  OUT_FOR_DELIVERY: "OUT_FOR_DELIVERY",
  DELIVERED: "DELIVERED",
  FAILED: "FAILED",
  RETURNED: "RETURNED",
} as const;
export type FulfillmentStatus = typeof FULFILLMENT_STATUS[keyof typeof FULFILLMENT_STATUS];

export function toShipmentStatus(s: FulfillmentStatus): ShipmentStatus {
  switch (s) {
    case "CREATED":
    case "PACKED": return "PACKING";
    case "SHIPPED": return "READY_TO_SHIP";
    case "IN_TRANSIT": return "IN_TRANSIT";
    case "OUT_FOR_DELIVERY": return "OUT_FOR_DELIVERY";
    case "DELIVERED": return "DELIVERED";
    case "FAILED":
    case "RETURNED": return "FAILED_DELIVERY";
  }
}

export interface CourierProvider {
  id: string;
  name: string;
  slug: string;
  logoUrl?: string;
  supportsCod: boolean;
  supportsReverse: boolean;
  trackingUrlTemplate?: string;
  active: boolean;
}

export interface PackageDimensions {
  lengthCm: number;
  widthCm: number;
  heightCm: number;
  weightGrams: number;
}

export interface ShipmentPackage {
  id: string;
  shipmentId: string;
  orderId: string;
  vendorId: string;
  itemIds: string[];
  dimensions: PackageDimensions;
  declaredValue: number;
  fragile: boolean;
  createdAt: string;
}

export interface ShipmentLeg {
  id: string;
  shipmentId: string;
  sequence: number;
  fromHub: string;
  toHub: string;
  courierId: string;
  status: FulfillmentStatus;
  trackingNumber: string | null;
  dispatchedAt: string | null;
  arrivedAt: string | null;
}

export type TrackingEventType =
  | "LABEL_GENERATED"
  | "PICKED_UP"
  | "ARRIVED_HUB"
  | "DEPARTED_HUB"
  | "OUT_FOR_DELIVERY"
  | "DELIVERED"
  | "FAILED_ATTEMPT"
  | "RETURNED";

export interface TrackingEvent {
  id: string;
  shipmentId: string;
  legId: string | null;
  type: TrackingEventType;
  status: FulfillmentStatus;
  location?: string;
  note?: string;
  at: string;
  raw?: Record<string, unknown>;
}

export function toShipmentTimelineEvent(e: TrackingEvent): ShipmentTimelineEvent {
  return {
    id: e.id,
    at: e.at,
    status: toShipmentStatus(e.status),
    location: e.location,
    note: e.note,
  };
}

export interface DeliveryEstimate {
  shipmentId: string;
  earliestAt: string;
  latestAt: string;
  confidence: number;
  generatedAt: string;
  source: "mock" | "courier" | "rule";
}

export interface ShippingLabel {
  id: string;
  shipmentId: string;
  courierId: string;
  trackingNumber: string;
  pdfUrl: string | null;
  createdAt: string;
  voidedAt: string | null;
}

export type FulfillmentTaskType =
  | "PACK"
  | "PRINT_LABEL"
  | "HANDOVER"
  | "REATTEMPT"
  | "PROCESS_RETURN";

export interface FulfillmentTask {
  id: string;
  shipmentId: string;
  vendorId: string;
  type: FulfillmentTaskType;
  status: "OPEN" | "IN_PROGRESS" | "DONE" | "BLOCKED";
  assigneeId: string | null;
  dueAt: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface ShipmentDetail {
  shipment: Shipment;
  packages: ShipmentPackage[];
  legs: ShipmentLeg[];
  events: TrackingEvent[];
  estimate: DeliveryEstimate | null;
  labels: ShippingLabel[];
  tasks: FulfillmentTask[];
}
