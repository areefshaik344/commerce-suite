import { installNotificationSubscriber } from "./subscribers/notificationSubscriber";
import { installAuditSubscriber } from "./subscribers/auditSubscriber";
import { installAnalyticsSubscriber } from "./subscribers/analyticsSubscriber";
import "./webhookOutbox";

let booted = false;

export function bootstrapPlatform(): void {
  if (booted) return;
  booted = true;
  installAuditSubscriber();
  installNotificationSubscriber();
  installAnalyticsSubscriber();
}

export { eventBus } from "./eventBus";
export { analyticsBus } from "./analyticsBus";
export { webhookOutbox } from "./webhookOutbox";