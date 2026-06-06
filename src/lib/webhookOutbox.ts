import { eventBus } from "./eventBus";
import { toWebhookDTO, type WebhookEventDTO } from "@/types/events";

const QUEUE_LIMIT = 500;
const queue: WebhookEventDTO[] = [];

eventBus.onAny((ev) => {
  queue.push(toWebhookDTO(ev));
  if (queue.length > QUEUE_LIMIT) queue.splice(0, queue.length - QUEUE_LIMIT);
});

export const webhookOutbox = {
  size(): number { return queue.length; },
  peek(): readonly WebhookEventDTO[] { return queue; },
  drain(): WebhookEventDTO[] { return queue.splice(0, queue.length); },
};