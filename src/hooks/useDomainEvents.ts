import { useEffect } from "react";
import { eventBus } from "@/lib/eventBus";
import type { DomainEventHandler, DomainEventType } from "@/types/events";

/** Subscribe to a typed domain event for the lifetime of the component. */
export function useDomainEvent<T extends DomainEventType>(
  type: T,
  handler: DomainEventHandler<T>,
): void {
  useEffect(() => eventBus.on(type, handler), [type, handler]);
}