/**
 * In-process domain EventBus.
 *
 * - Typed pub/sub with per-event-type handler lists
 * - Wildcard subscribers (audit, analytics, dev logger, webhook forwarder)
 * - Bounded history ring buffer for debugging & replay
 *
 * No external libraries. Backend will later forward identical envelopes
 * to message-queue consumers / webhook endpoints.
 */

import type {
  AnyDomainEvent,
  DomainEventHandler,
  DomainEventMap,
  DomainEventType,
  DomainEventActor,
} from "@/types/events";

type WildcardHandler = (event: AnyDomainEvent) => void | Promise<void>;

const HISTORY_LIMIT = 200;

class EventBus {
  private handlers = new Map<DomainEventType, Set<DomainEventHandler<DomainEventType>>>();
  private wildcard = new Set<WildcardHandler>();
  private history: AnyDomainEvent[] = [];

  on<T extends DomainEventType>(type: T, handler: DomainEventHandler<T>): () => void {
    if (!this.handlers.has(type)) this.handlers.set(type, new Set());
    this.handlers.get(type)!.add(handler as DomainEventHandler<DomainEventType>);
    return () => this.off(type, handler);
  }

  off<T extends DomainEventType>(type: T, handler: DomainEventHandler<T>): void {
    this.handlers.get(type)?.delete(handler as DomainEventHandler<DomainEventType>);
  }

  onAny(handler: WildcardHandler): () => void {
    this.wildcard.add(handler);
    return () => this.wildcard.delete(handler);
  }

  publish<T extends DomainEventType>(
    type: T,
    payload: DomainEventMap[T]["payload"],
    opts: { actor?: DomainEventActor; correlationId?: string; source?: string } = {}
  ): DomainEventMap[T] {
    const envelope = {
      id: makeEventId(),
      type,
      occurredAt: new Date().toISOString(),
      source: opts.source ?? inferSource(type),
      version: 1 as const,
      actor: opts.actor,
      correlationId: opts.correlationId,
      payload,
    } as DomainEventMap[T];

    this.recordHistory(envelope);
    this.dispatch(envelope);
    return envelope;
  }

  /** Re-publish an existing envelope (used by backend ingest / replay). */
  republish(envelope: AnyDomainEvent): void {
    this.recordHistory(envelope);
    this.dispatch(envelope);
  }

  history_(): readonly AnyDomainEvent[] {
    return this.history;
  }

  clearHistory(): void {
    this.history = [];
  }

  private dispatch(envelope: AnyDomainEvent): void {
    const typed = this.handlers.get(envelope.type);
    typed?.forEach((h) => safeRun(() => h(envelope)));
    this.wildcard.forEach((h) => safeRun(() => h(envelope)));
  }

  private recordHistory(envelope: AnyDomainEvent): void {
    this.history.push(envelope);
    if (this.history.length > HISTORY_LIMIT) {
      this.history.splice(0, this.history.length - HISTORY_LIMIT);
    }
  }
}

function safeRun(fn: () => void | Promise<void>): void {
  try {
    const r = fn();
    if (r && typeof (r as Promise<void>).catch === "function") {
      (r as Promise<void>).catch((err) => {
        if (import.meta.env.DEV) console.error("[eventBus] handler error", err);
      });
    }
  } catch (err) {
    if (import.meta.env.DEV) console.error("[eventBus] handler error", err);
  }
}

function makeEventId(): string {
  return `evt_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`;
}

function inferSource(type: DomainEventType): string {
  if (type.startsWith("ORDER_")) return "orders";
  if (type.startsWith("SHIPMENT_")) return "shipping";
  if (type.startsWith("PAYMENT_")) return "payments";
  if (type.startsWith("RETURN_")) return "returns";
  if (type.startsWith("REFUND_")) return "refunds";
  if (type.startsWith("VENDOR_")) return "vendors";
  if (type.startsWith("USER_")) return "users";
  return "platform";
}

export const eventBus = new EventBus();

/** Dev-only logger, enabled automatically in development builds. */
if (import.meta.env.DEV) {
  eventBus.onAny((ev) => {
    // eslint-disable-next-line no-console
    console.debug(`[domain-event] ${ev.type}`, ev);
  });
}