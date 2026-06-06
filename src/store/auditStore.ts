import { create } from "zustand";
import type { AuditRecord } from "@/types/audit";

const BUFFER_LIMIT = 500;

interface AuditState {
  records: AuditRecord[];
  append: (record: Omit<AuditRecord, "id" | "timestamp"> & { id?: string; timestamp?: string }) => AuditRecord;
  clear: () => void;
  byEntity: (entityType: AuditRecord["entityType"], entityId: string) => AuditRecord[];
}

export const useAuditStore = create<AuditState>((set, get) => ({
  records: [],
  append: (input) => {
    const record: AuditRecord = {
      id: input.id ?? `aud_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`,
      timestamp: input.timestamp ?? new Date().toISOString(),
      severity: input.severity ?? "info",
      actor: input.actor,
      action: input.action,
      entityType: input.entityType,
      entityId: input.entityId,
      message: input.message,
      meta: input.meta,
      correlationId: input.correlationId,
    };
    set((s) => {
      const next = [record, ...s.records];
      if (next.length > BUFFER_LIMIT) next.length = BUFFER_LIMIT;
      return { records: next };
    });
    return record;
  },
  clear: () => set({ records: [] }),
  byEntity: (entityType, entityId) =>
    get().records.filter((r) => r.entityType === entityType && r.entityId === entityId),
}));