import type { OrderDraft } from "@/types/checkout";
import type {
  OrderRecord, OrderItem, Shipment, VendorOrder, PaymentRecord,
  OrderTimelineEvent, OrderShippingAddressSnapshot, ProductSnapshot, VendorSnapshot, PricingSnapshot,
} from "@/types/order";
import { ORDER_STATUS, SHIPMENT_STATUS, PAYMENT_STATUS } from "@/types/order";

/**
 * Build an immutable OrderRecord from a checkout OrderDraft.
 * Everything is snapshotted — order is fully self-contained.
 */

let _id = 0;
const seq = (prefix: string) => `${prefix}-${Date.now().toString(36)}-${(++_id).toString(36)}`;

function snapshotAddress(a: OrderDraft["address"], at: string): OrderShippingAddressSnapshot {
  return { ...a, capturedAt: at };
}

export function buildOrderFromDraft(draft: OrderDraft, orderId: string, placedAt: string): OrderRecord {
  const now = placedAt;
  const items: OrderItem[] = [];
  const shipments: Shipment[] = [];
  const vendorOrders: VendorOrder[] = [];
  const timeline: OrderTimelineEvent[] = [];

  for (const ship of draft.shipments) {
    const vendor: VendorSnapshot = { vendorId: ship.vendorId, vendorName: ship.vendorName };
    const vendorOrderId = seq("VO");
    const shipmentId = seq("SHP");
    const itemIds: string[] = [];

    for (const line of ship.items) {
      const itemId = seq("OI");
      const productSnap: ProductSnapshot = {
        productId: line.productId,
        variantId: line.variantId,
        sku: line.variantId ?? line.productId,
        name: line.productId,
        image: "",
      };
      const subtotal = line.unitPrice * line.quantity;
      const pricing: PricingSnapshot = {
        unitPrice: line.unitPrice,
        quantity: line.quantity,
        subtotal,
        discount: 0,
        tax: 0,
        total: subtotal,
        currency: "INR",
      };
      items.push({
        id: itemId, product: productSnap, vendor, pricing,
        status: "ACTIVE", shipmentId,
        cancelledQuantity: 0, returnedQuantity: 0, refundedAmount: 0,
      });
      itemIds.push(itemId);
    }

    shipments.push({
      id: shipmentId, orderId, vendorOrderId, vendorId: ship.vendorId,
      status: SHIPMENT_STATUS.PACKING,
      trackingNumber: null, carrier: null,
      methodId: ship.shipping.methodId, shippingCost: ship.shipping.cost,
      estimatedDeliveryAt: new Date(Date.now() + ship.shipping.estimatedDays * 86400_000).toISOString(),
      shippedAt: null, deliveredAt: null,
      itemIds, timeline: [], createdAt: now, updatedAt: now,
    });

    vendorOrders.push({
      id: vendorOrderId, orderId, vendor, itemIds, shipmentIds: [shipmentId],
      status: ORDER_STATUS.CONFIRMED,
      subtotal: ship.subtotal, discount: ship.discount, shipping: ship.shipping.cost,
      tax: ship.tax, total: ship.total,
    });
  }

  const payment: PaymentRecord = {
    id: seq("PAY"),
    orderId,
    methodId: draft.payment.methodId,
    status: draft.payment.methodId === "cod" ? PAYMENT_STATUS.PENDING : PAYMENT_STATUS.CAPTURED,
    amount: draft.pricing.grandTotal,
    refundedAmount: 0,
    gatewayRef: draft.payment.methodId === "cod" ? null : draft.payment.token ?? `MOCK-${orderId}`,
    capturedAt: draft.payment.methodId === "cod" ? null : now,
    createdAt: now, updatedAt: now,
  };

  timeline.push({
    id: seq("EV"), orderId, type: "ORDER_PLACED", at: now,
    actor: { id: draft.ownerId, role: "customer" },
    message: "Order placed successfully",
  });

  return {
    id: orderId,
    customerId: draft.ownerId,
    status: ORDER_STATUS.CONFIRMED,
    items, vendorOrders, shipments, timeline,
    payment, refunds: [], cancellations: [], returns: [],
    shippingAddress: snapshotAddress(draft.address, now),
    pricing: draft.pricing,
    reservationId: draft.reservationId,
    placedAt: now, createdAt: now, updatedAt: now,
    cancelledAt: null, deliveredAt: null,
  };
}