import { useMemo, useCallback } from "react";
import { useAuthStore } from "@/store/authStore";
import {
  can as canFn,
  hasRole as hasRoleFn,
  ownsResource as ownsResourceFn,
  canManageResource,
  toAppRole,
  isAdminRole,
  getEffectivePermissions,
  type Permission,
} from "@/lib/permissions";
import {
  resolveAccountStatus,
  getStatusBanner,
  getProfileCompletion,
  canPerform,
  type ActionKey,
} from "@/lib/accountStatus";
import type { AppRole, UserRole } from "@/data/mock-users";

/**
 * The single hook UI components should use for any authorisation question.
 * Centralising this prevents scattered `role === "x"` checks and lets us
 * change the underlying model (add a role, swap to backend-supplied perms,
 * etc.) without touching components.
 */
export function usePermissions() {
  const user = useAuthStore((s) => s.currentUser);

  const permissions = useMemo(() => getEffectivePermissions(user), [user]);
  const accountStatus = useMemo(() => resolveAccountStatus(user), [user]);
  const banner = useMemo(() => getStatusBanner(user), [user]);
  const completion = useMemo(() => getProfileCompletion(user), [user]);

  const can = useCallback((perm: Permission | Permission[] | { all: Permission[] }) => canFn(user, perm), [user]);
  const hasRole = useCallback((role: AppRole | AppRole[] | UserRole | UserRole[]) => hasRoleFn(user, role), [user]);
  const ownsResource = useCallback((ownerId: string | null | undefined) => ownsResourceFn(user, ownerId), [user]);
  const canManage = useCallback(
    (ownerId: string | null | undefined, perm: Permission) => canManageResource(user, ownerId, perm),
    [user]
  );
  const permit = useCallback((action: ActionKey) => canPerform(user, action), [user]);

  return {
    user,
    role: toAppRole(user?.role),
    isAdmin: isAdminRole(user?.role),
    isAuthenticated: !!user,
    permissions,
    accountStatus,
    banner,
    completion,
    can,
    hasRole,
    ownsResource,
    canManage,
    permit,
  };
}