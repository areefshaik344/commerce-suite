import type {
  OrderRecord, OrderItem, Shipment, VendorOrder, PaymentRecord,
  OrderTimelineEvent, ProductSnapshot, VendorSnapshot,
} from "@/types/order";
import { ORDER_STATUS, SHIPMENT_STATUS, PAYMENT_STATUS } from "@/types/order";
import type { Order as LegacyOrder } from "@/data/mock-orders";
import { mockOrders as legacyOrders } from "./mockOrders";
import { mockUsers, mockVendors } from "./mockUsers";
import type { PricingBreakdown } from "@/types/checkout";

const legacyToOrderStatus: Record<LegacyOrder["status"], OrderRecord["status"]> = {
  pending:   ORDER_STATUS.CREATED,
  confirmed: ORDER_STATUS.CONFIRMED,
  shipped:   ORDER_STATUS.SHIPPED,
  delivered: ORDER_STATUS.DELIVERED,
  cancelled: ORDER_STATUS.CANCELLED,
  returned:  ORDER_STATUS.RETURNED,
};

const legacyToShipmentStatus: Record<LegacyOrder["status"], OrderRecord["shipments"][number]["status"]> = {
  pending:   SHIPMENT_STATUS.PACKING,
  confirmed: SHIPMENT_STATUS.READY_TO_SHIP,
  shipped:   SHIPMENT_STATUS.IN_TRANSIT,
  delivered: SHIPMENT_STATUS.DELIVERED,
  cancelled: SHIPMENT_STATUS.PACKING,
  returned:  SHIPMENT_STATUS.DELIVERED,
};

function adapt(o: LegacyOrder): OrderRecord {
  const vendor = mockVendors.find(v => v.id === o.vendorId);
  const vendorSnap: VendorSnapshot = {
    vendorId: o.vendorId,
    vendorName: vendor?.businessName ?? vendor?.name ?? "Seller",
  };
  const items: OrderItem[] = o.items.map((it, idx) => {
    const productSnap: ProductSnapshot = {
      productId: it.productId,
      variantId: null,
      sku: it.productId,
      name: it.productName,
      image: it.image || "",
    };
    return {
      id: `${o.id}-IT-${idx + 1}`,
      product: productSnap,
      vendor: vendorSnap,
      pricing: {
        unitPrice: it.price,
        quantity: it.quantity,
        subtotal: it.price * it.quantity,
        discount: 0,
        tax: 0,
        total: it.price * it.quantity,
        currency: "INR",
      },
      status: o.status === "cancelled" ? "CANCELLED" : o.status === "returned" ? "RETURNED" : "ACTIVE",
      shipmentId: `${o.id}-SHP-1`,
      cancelledQuantity: o.status === "cancelled" ? it.quantity : 0,
      returnedQuantity:  o.status === "returned"  ? it.quantity : 0,
      refundedAmount:    o.status === "returned" || o.status === "cancelled" ? it.price * it.quantity : 0,
    };
  });

  const shipment: Shipment = {
    id: `${o.id}-SHP-1`,
    orderId: o.id,
    vendorOrderId: `${o.id}-VO-1`,
    vendorId: o.vendorId,
    status: legacyToShipmentStatus[o.status],
    trackingNumber: o.trackingId ?? null,
    carrier: o.trackingId ? "BlueDart" : null,
    methodId: "standard",
    shippingCost: 0,
    estimatedDeliveryAt: new Date(new Date(o.createdAt).getTime() + 5 * 86400_000).toISOString(),
    shippedAt: ["shipped", "delivered"].includes(o.status) ? o.updatedAt : null,
    deliveredAt: o.status === "delivered" ? o.updatedAt : null,
    itemIds: items.map(i => i.id),
    timeline: [],
    createdAt: o.createdAt,
    updatedAt: o.updatedAt,
  };

  const vendorOrder: VendorOrder = {
    id: `${o.id}-VO-1`,
    orderId: o.id,
    vendor: vendorSnap,
    itemIds: items.map(i => i.id),
    shipmentIds: [shipment.id],
    status: legacyToOrderStatus[o.status],
    subtotal: o.total, discount: 0, shipping: 0, tax: 0, total: o.total,
  };

  const payment: PaymentRecord = {
    id: `${o.id}-PAY-1`,
    orderId: o.id,
    methodId: (o.paymentMethod.toLowerCase().includes("cod") ? "cod" :
               o.paymentMethod.toLowerCase().includes("upi") ? "upi" :
               o.paymentMethod.toLowerCase().includes("card") ? "card" : "wallet"),
    status: o.status === "cancelled" ? PAYMENT_STATUS.REFUNDED :
            o.status === "returned"  ? PAYMENT_STATUS.REFUNDED :
            o.status === "pending"   ? PAYMENT_STATUS.PENDING : PAYMENT_STATUS.CAPTURED,
    amount: o.total,
    refundedAmount: ["cancelled", "returned"].includes(o.status) ? o.total : 0,
    gatewayRef: `MOCK-${o.id}`,
    capturedAt: o.status === "pending" ? null : o.createdAt,
    createdAt: o.createdAt,
    updatedAt: o.updatedAt,
  };

  const timeline: OrderTimelineEvent[] = [
    { id: `${o.id}-EV-1`, orderId: o.id, type: "ORDER_PLACED", at: o.createdAt,
      actor: { id: o.userId, role: "customer" }, message: "Order placed" },
  ];
  if (["confirmed","shipped","delivered"].includes(o.status)) {
    timeline.push({ id: `${o.id}-EV-2`, orderId: o.id, type: "ORDER_CONFIRMED", at: o.updatedAt,
      actor: { id: o.vendorId, role: "vendor" }, message: "Order confirmed by seller" });
  }
  if (["shipped","delivered"].includes(o.status)) {
    timeline.push({ id: `${o.id}-EV-3`, orderId: o.id, type: "SHIPMENT_DISPATCHED", at: o.updatedAt,
      actor: { id: "system", role: "system" }, message: `Shipment dispatched${o.trackingId ? ` (${o.trackingId})` : ""}` });
  }
  if (o.status === "delivered") {
    timeline.push({ id: `${o.id}-EV-4`, orderId: o.id, type: "SHIPMENT_DELIVERED", at: o.updatedAt,
      actor: { id: "system", role: "system" }, message: "Delivered" });
  }
  if (o.status === "cancelled") {
    timeline.push({ id: `${o.id}-EV-c`, orderId: o.id, type: "CANCELLED", at: o.updatedAt,
      actor: { id: o.userId, role: "customer" }, message: "Order cancelled" });
  }

  const pricing: PricingBreakdown = {
    subtotal: o.total, discount: 0, shipping: 0, tax: 0, platformFee: 0,
    grandTotal: o.total,
    vendorBreakdowns: [{
      vendorId: o.vendorId, vendorName: vendorSnap.vendorName,
      itemCount: items.reduce((a, i) => a + i.pricing.quantity, 0),
      subtotal: o.total, discount: 0, shipping: 0, tax: 0, total: o.total,
    }],
    appliedCoupons: [],
    currency: "INR",
    computedAt: new Date(o.createdAt).getTime(),
  };

  const user = mockUsers.find(u => u.id === o.userId);
  const fallbackAddress = {
    id: "legacy-addr",
    label: "Home" as const,
    name: user?.name ?? "Customer",
    phone: user?.phone ?? "",
    line1: o.shippingAddress,
    line2: "",
    city: "", state: "", pincode: "", country: "India",
    isDefault: true,
  };
  const addrSrc = user?.addresses?.[0] ?? fallbackAddress;

  return {
    id: o.id,
    customerId: o.userId,
    status: legacyToOrderStatus[o.status],
    items, vendorOrders: [vendorOrder], shipments: [shipment], timeline,
    payment, refunds: [], cancellations: [], returns: [],
    shippingAddress: { ...addrSrc, capturedAt: o.createdAt },
    pricing,
    reservationId: null,
    placedAt: o.createdAt,
    createdAt: o.createdAt,
    updatedAt: o.updatedAt,
    cancelledAt: o.status === "cancelled" ? o.updatedAt : null,
    deliveredAt: o.status === "delivered" ? o.updatedAt : null,
  };
}

export const mockOrderRecords: OrderRecord[] = legacyOrders.map(adapt);