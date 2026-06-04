import type {
  OrderRecord, OrderItem, Shipment, VendorOrder, OrderStatus,
} from "@/types/order";
import { ORDER_STATUS, SHIPMENT_STATUS, isShippedShipment } from "@/types/order";

/**
 * Pure selectors over OrderRecord. Memoize at call sites via `useMemo`.
 */

export function getItemsByVendor(order: OrderRecord): Record<string, OrderItem[]> {
  const out: Record<string, OrderItem[]> = {};
  for (const it of order.items) {
    (out[it.vendor.vendorId] ??= []).push(it);
  }
  return out;
}

export function getShipmentForItem(order: OrderRecord, itemId: string): Shipment | null {
  const item = order.items.find(i => i.id === itemId);
  if (!item?.shipmentId) return null;
  return order.shipments.find(s => s.id === item.shipmentId) ?? null;
}

export function getItemsForShipment(order: OrderRecord, shipmentId: string): OrderItem[] {
  const s = order.shipments.find(x => x.id === shipmentId);
  if (!s) return [];
  const set = new Set(s.itemIds);
  return order.items.filter(i => set.has(i.id));
}

export function getVendorOrder(order: OrderRecord, vendorOrderId: string): VendorOrder | null {
  return order.vendorOrders.find(v => v.id === vendorOrderId) ?? null;
}

export function getActiveItemQuantity(item: OrderItem): number {
  return Math.max(0, item.pricing.quantity - item.cancelledQuantity - item.returnedQuantity);
}

/* -------------------------------------------------------------------------- */
/* Eligibility                                                                */
/* -------------------------------------------------------------------------- */

export interface CancelEligibility {
  eligible: boolean;
  itemIds: string[];
  reason?: string;
}

/** Items can be cancelled while not shipped yet and still active. */
export function getCancellableItems(order: OrderRecord): CancelEligibility {
  if (order.status === ORDER_STATUS.CANCELLED || order.status === ORDER_STATUS.DELIVERED ||
      order.status === ORDER_STATUS.RETURNED || order.status === ORDER_STATUS.REFUNDED) {
    return { eligible: false, itemIds: [], reason: "Order is no longer cancellable" };
  }
  const eligible = order.items.filter(it => {
    if (it.status !== "ACTIVE") return false;
    if (getActiveItemQuantity(it) <= 0) return false;
    const shipment = it.shipmentId ? order.shipments.find(s => s.id === it.shipmentId) : null;
    if (shipment && isShippedShipment(shipment.status)) return false;
    return true;
  });
  return { eligible: eligible.length > 0, itemIds: eligible.map(i => i.id) };
}

/** Window — delivered items can be returned within N days. */
export const RETURN_WINDOW_DAYS = 7;

export interface ReturnEligibility {
  eligible: boolean;
  itemIds: string[];
  reason?: string;
  windowEndsAt?: string;
}

export function getReturnableItems(order: OrderRecord, now: Date = new Date()): ReturnEligibility {
  const eligible: string[] = [];
  let windowEndsAt: string | undefined;
  for (const it of order.items) {
    if (it.status !== "ACTIVE") continue;
    if (getActiveItemQuantity(it) <= 0) continue;
    const shipment = it.shipmentId ? order.shipments.find(s => s.id === it.shipmentId) : null;
    if (!shipment || shipment.status !== SHIPMENT_STATUS.DELIVERED || !shipment.deliveredAt) continue;
    const delivered = new Date(shipment.deliveredAt);
    const ends = new Date(delivered.getTime() + RETURN_WINDOW_DAYS * 86400_000);
    if (now > ends) continue;
    eligible.push(it.id);
    if (!windowEndsAt || new Date(windowEndsAt) > ends) windowEndsAt = ends.toISOString();
  }
  return {
    eligible: eligible.length > 0,
    itemIds: eligible,
    windowEndsAt,
    reason: eligible.length === 0 ? "No delivered items within return window" : undefined,
  };
}

/* -------------------------------------------------------------------------- */
/* Derived totals                                                             */
/* -------------------------------------------------------------------------- */

export interface OrderTotalsSummary {
  itemCount: number;
  activeItemCount: number;
  shipmentCount: number;
  delivered: number;
  inTransit: number;
  pending: number;
  refundedAmount: number;
}

export function summarizeOrder(order: OrderRecord): OrderTotalsSummary {
  let active = 0, refunded = 0;
  for (const it of order.items) {
    active += getActiveItemQuantity(it);
    refunded += it.refundedAmount;
  }
  return {
    itemCount: order.items.reduce((a, i) => a + i.pricing.quantity, 0),
    activeItemCount: active,
    shipmentCount: order.shipments.length,
    delivered: order.shipments.filter(s => s.status === SHIPMENT_STATUS.DELIVERED).length,
    inTransit: order.shipments.filter(s => isShippedShipment(s.status) && s.status !== SHIPMENT_STATUS.DELIVERED).length,
    pending: order.shipments.filter(s => s.status === SHIPMENT_STATUS.PACKING || s.status === SHIPMENT_STATUS.READY_TO_SHIP).length,
    refundedAmount: refunded,
  };
}

/* -------------------------------------------------------------------------- */
/* Aggregate status from shipments                                            */
/* -------------------------------------------------------------------------- */

export function deriveOrderStatusFromShipments(order: OrderRecord): OrderStatus {
  if (order.status === ORDER_STATUS.CANCELLED || order.status === ORDER_STATUS.REFUNDED ||
      order.status === ORDER_STATUS.RETURNED) {
    return order.status;
  }
  if (order.shipments.length === 0) return order.status;
  const allDelivered = order.shipments.every(s => s.status === SHIPMENT_STATUS.DELIVERED);
  if (allDelivered) return ORDER_STATUS.DELIVERED;
  const anyShipped = order.shipments.some(s => isShippedShipment(s.status));
  const allShipped = order.shipments.every(s => isShippedShipment(s.status));
  if (allShipped) return ORDER_STATUS.SHIPPED;
  if (anyShipped) return ORDER_STATUS.PARTIALLY_SHIPPED;
  return ORDER_STATUS.PROCESSING;
}