import { eventBus } from "@/lib/eventBus";
import { notificationApi } from "@/api/notificationApi";
import type { NotificationEventType, DeliveryChannel } from "@/types/notification";

let installed = false;

function emit(
  type: NotificationEventType,
  title: string,
  message: string,
  actionUrl?: string,
  channels: DeliveryChannel[] = ["in_app", "email"],
  meta?: Record<string, unknown>,
) {
  void notificationApi.emit({ type, title, message, actionUrl, channels, meta });
}

export function installNotificationSubscriber(): void {
  if (installed) return;
  installed = true;

  eventBus.on("ORDER_CREATED", (ev) =>
    emit("order.placed", "Order placed", `Order ${ev.payload.orderId} placed successfully.`, `/orders/${ev.payload.orderId}`),
  );
  eventBus.on("ORDER_CANCELLED", (ev) =>
    emit("order.cancelled", "Order cancelled", `Order ${ev.payload.orderId} was cancelled.`, `/orders/${ev.payload.orderId}`),
  );
  eventBus.on("ORDER_DELIVERED", (ev) =>
    emit("shipment.delivered", "Order delivered", `Order ${ev.payload.orderId} has been delivered.`, `/orders/${ev.payload.orderId}`),
  );
  eventBus.on("SHIPMENT_CREATED", (ev) =>
    emit("shipment.shipped", "Shipment created", `Tracking ${ev.payload.trackingNumber ?? ev.payload.shipmentId} created.`, `/tracking/${ev.payload.shipmentId}`),
  );
  eventBus.on("SHIPMENT_UPDATED", (ev) =>
    emit("shipment.out_for_delivery", "Shipment update", `Status: ${ev.payload.status}.`, `/tracking/${ev.payload.shipmentId}`),
  );
  eventBus.on("PAYMENT_CAPTURED", (ev) =>
    emit("payment.succeeded", "Payment captured", `${ev.payload.currency} ${ev.payload.amount} captured for ${ev.payload.orderId}.`, `/payments/${ev.payload.paymentId}`),
  );
  eventBus.on("PAYMENT_FAILED", (ev) =>
    emit("payment.failed", "Payment failed", ev.payload.reason, `/payments/${ev.payload.paymentId}`, ["in_app", "email", "sms"]),
  );
  eventBus.on("RETURN_REQUESTED", (ev) =>
    emit("return.requested", "Return requested", `Return ${ev.payload.returnId} submitted.`, `/orders/${ev.payload.orderId}`),
  );
  eventBus.on("RETURN_APPROVED", (ev) =>
    emit("return.approved", "Return approved", `Return ${ev.payload.returnId} approved.`, `/orders/${ev.payload.orderId}`),
  );
  eventBus.on("REFUND_ISSUED", (ev) =>
    emit("refund.completed", "Refund issued", `${ev.payload.currency} ${ev.payload.amount} refunded for ${ev.payload.orderId}.`, `/orders/${ev.payload.orderId}`),
  );
  eventBus.on("VENDOR_APPROVED", (ev) =>
    emit("vendor.application_update", "Vendor approved", `Vendor ${ev.payload.vendorId} approved.`),
  );
  eventBus.on("USER_SUSPENDED", (ev) =>
    emit("system.security", "Account suspended", ev.payload.reason ?? "Your account has been suspended.", undefined, ["in_app", "email"]),
  );
}