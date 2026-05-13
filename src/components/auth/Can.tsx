import { ReactNode } from "react";
import { usePermissions } from "@/hooks/usePermissions";
import type { Permission } from "@/lib/permissions";

interface BaseProps {
  children: ReactNode;
  fallback?: ReactNode;
}

type Props = BaseProps & (
  | { perm: Permission | Permission[]; all?: Permission[]; ownerId?: string }
  | { all: Permission[]; perm?: never; ownerId?: string }
);

/**
 * Declarative permission gate.
 *
 *   <Can perm="MANAGE_PRODUCTS">…</Can>
 *   <Can perm={["A","B"]}>…</Can>                            // ANY-of
 *   <Can all={["A","B"]}>…</Can>                             // ALL-of
 *   <Can perm="MANAGE_PRODUCTS" ownerId={p.ownerId}>…</Can>  // ownership-scoped
 */
export function Can({ children, fallback = null, perm, all, ownerId }: Props) {
  const { can, canManage } = usePermissions();
  let allowed = false;
  if (all) allowed = can({ all });
  else if (perm && ownerId) {
    allowed = Array.isArray(perm)
      ? perm.some((p) => canManage(ownerId, p))
      : canManage(ownerId, perm);
  } else if (perm) allowed = can(perm);
  return <>{allowed ? children : fallback}</>;
}

export default Can;