import type {
  AnalyticsEvent,
  AnalyticsEnvelope,
  AnalyticsContext,
} from "@/types/analyticsEvents";

type Listener = (env: AnalyticsEnvelope) => void;

class AnalyticsBus {
  private listeners = new Set<Listener>();
  private context: AnalyticsContext = {};

  setContext(patch: Partial<AnalyticsContext>): void {
    this.context = { ...this.context, ...patch };
  }

  on(listener: Listener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  track(event: AnalyticsEvent): AnalyticsEnvelope {
    const env: AnalyticsEnvelope = {
      event,
      context: {
        ...this.context,
        url: typeof window !== "undefined" ? window.location.href : undefined,
      },
      at: new Date().toISOString(),
    };
    this.listeners.forEach((l) => {
      try { l(env); } catch (err) {
        if (import.meta.env.DEV) console.error("[analyticsBus] listener error", err);
      }
    });
    return env;
  }
}

export const analyticsBus = new AnalyticsBus();

if (import.meta.env.DEV) {
  analyticsBus.on((env) => {
    console.debug(`[analytics] ${env.event.name}`, env.event);
  });
}