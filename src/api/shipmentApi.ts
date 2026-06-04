import { simulateDelay, mockSuccess, ApiError, type ApiResponse } from "./apiClient";
import type { OrderRecord, Shipment, ShipmentStatus, ShipmentTimelineEvent } from "@/types/order";
import { SHIPMENT_STATUS, ORDER_STATUS } from "@/types/order";
import { canTransitionShipment } from "@/lib/orderStatus";
import { deriveOrderStatusFromShipments } from "@/lib/orderSelectors";
import { mockOrderRecords } from "@/mocks/mockOrderRecords";

let DATA: OrderRecord[] = mockOrderRecords;
export function __bindShipmentDataset(d: OrderRecord[]) { DATA = d; }

const seq = (p: string) => `${p}-${Date.now().toString(36)}-${Math.floor(Math.random()*1e4).toString(36)}`;

export const shipmentApi = {
  async getById(shipmentId: string): Promise<ApiResponse<Shipment>> {
    await simulateDelay(150);
    for (const o of DATA) {
      const s = o.shipments.find(x => x.id === shipmentId);
      if (s) return mockSuccess(s);
    }
    throw new ApiError("Shipment not found", 404, "SHIPMENT_NOT_FOUND");
  },

  async updateStatus(input: {
    orderId: string; shipmentId: string; status: ShipmentStatus;
    location?: string; note?: string; trackingNumber?: string; carrier?: string;
    actorId: string; actorRole: "vendor" | "admin";
  }): Promise<ApiResponse<{ order: OrderRecord; shipment: Shipment }>> {
    await simulateDelay(300);
    const order = DATA.find(o => o.id === input.orderId);
    if (!order) throw new ApiError("Order not found", 404, "ORDER_NOT_FOUND");
    const shipment = order.shipments.find(s => s.id === input.shipmentId);
    if (!shipment) throw new ApiError("Shipment not found", 404, "SHIPMENT_NOT_FOUND");
    if (!canTransitionShipment(shipment.status, input.status)) {
      throw new ApiError(`Invalid transition ${shipment.status} → ${input.status}`, 409, "INVALID_TRANSITION");
    }
    const now = new Date().toISOString();
    const event: ShipmentTimelineEvent = {
      id: seq("STEV"), at: now, status: input.status, location: input.location, note: input.note,
    };
    const nextShipment: Shipment = {
      ...shipment,
      status: input.status,
      trackingNumber: input.trackingNumber ?? shipment.trackingNumber,
      carrier: input.carrier ?? shipment.carrier,
      shippedAt: input.status === SHIPMENT_STATUS.IN_TRANSIT && !shipment.shippedAt ? now : shipment.shippedAt,
      deliveredAt: input.status === SHIPMENT_STATUS.DELIVERED ? now : shipment.deliveredAt,
      timeline: [...shipment.timeline, event],
      updatedAt: now,
    };
    const shipments = order.shipments.map(s => s.id === shipment.id ? nextShipment : s);
    let nextOrder: OrderRecord = { ...order, shipments, updatedAt: now };
    nextOrder = {
      ...nextOrder,
      status: deriveOrderStatusFromShipments(nextOrder),
      deliveredAt: nextOrder.status === ORDER_STATUS.DELIVERED ? now : nextOrder.deliveredAt,
    };
    nextOrder.timeline = [...nextOrder.timeline, {
      id: seq("EV"), orderId: order.id,
      type: input.status === SHIPMENT_STATUS.IN_TRANSIT ? "SHIPMENT_DISPATCHED" :
            input.status === SHIPMENT_STATUS.OUT_FOR_DELIVERY ? "SHIPMENT_OUT_FOR_DELIVERY" :
            input.status === SHIPMENT_STATUS.DELIVERED ? "SHIPMENT_DELIVERED" :
            input.status === SHIPMENT_STATUS.FAILED_DELIVERY ? "SHIPMENT_FAILED" : "NOTE",
      at: now, actor: { id: input.actorId, role: input.actorRole },
      message: `Shipment ${input.shipmentId} → ${input.status}`,
    }];
    const idx = DATA.findIndex(o => o.id === order.id);
    if (idx >= 0) DATA[idx] = nextOrder;
    return mockSuccess({ order: nextOrder, shipment: nextShipment });
  },
};