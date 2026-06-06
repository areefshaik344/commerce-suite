import { eventBus } from "@/lib/eventBus";
import { analyticsBus } from "@/lib/analyticsBus";

let installed = false;

export function installAnalyticsSubscriber(): void {
  if (installed) return;
  installed = true;

  eventBus.on("ORDER_CREATED", (ev) => {
    analyticsBus.track({
      name: "order_placed",
      orderId: ev.payload.orderId,
      total: ev.payload.total,
      itemCount: ev.payload.itemCount,
      currency: ev.payload.currency,
      vendorIds: ev.payload.vendorIds,
    });
  });

  eventBus.on("PAYMENT_CAPTURED", (ev) => {
    analyticsBus.track({
      name: "payment_completed",
      paymentId: ev.payload.paymentId,
      orderId: ev.payload.orderId,
      amount: ev.payload.amount,
      currency: ev.payload.currency,
      method: "captured",
    });
  });
}