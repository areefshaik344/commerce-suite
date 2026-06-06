import { mockSuccess, simulateDelay, type ApiResponse } from "./apiClient";
import { useAuditStore } from "@/store/auditStore";
import type { AuditRecord } from "@/types/audit";

export const auditApi = {
  async record(input: Omit<AuditRecord, "id" | "timestamp">): Promise<ApiResponse<AuditRecord>> {
    await simulateDelay(40);
    const rec = useAuditStore.getState().append(input);
    return mockSuccess(rec, "Audit recorded");
  },

  async list(opts: { entityType?: AuditRecord["entityType"]; entityId?: string; actorId?: string; limit?: number } = {}): Promise<ApiResponse<AuditRecord[]>> {
    await simulateDelay(120);
    let items = useAuditStore.getState().records;
    if (opts.entityType) items = items.filter((r) => r.entityType === opts.entityType);
    if (opts.entityId) items = items.filter((r) => r.entityId === opts.entityId);
    if (opts.actorId) items = items.filter((r) => r.actor.id === opts.actorId);
    if (opts.limit) items = items.slice(0, opts.limit);
    return mockSuccess(items);
  },
};