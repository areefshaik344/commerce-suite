import { Navigate, useLocation } from "react-router-dom";
import { useAuthStore } from "@/store/authStore";
import { can as canFn, type Permission } from "@/lib/permissions";
import { resolveAccountStatus } from "@/lib/accountStatus";
import PermissionDenied from "@/components/auth/PermissionDenied";

interface Props {
  perm: Permission | Permission[] | { all: Permission[] };
  children: React.ReactNode;
  /** When true, render <PermissionDenied/> inline instead of redirecting. */
  inline?: boolean;
}

/**
 * Permission-gated route. Use INSTEAD of role-only `<ProtectedRoute>` when a
 * route is bound to a capability rather than a role — e.g. an admin sub-page
 * that should also work for FINANCE_ADMIN.
 */
export default function PermissionRoute({ perm, children, inline }: Props) {
  const { isAuthenticated, currentUser, isBootstrapping } = useAuthStore();
  const location = useLocation();

  if (isBootstrapping) return null;
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />;
  }

  const status = resolveAccountStatus(currentUser);
  if (status === "BANNED" || status === "SUSPENDED" || status === "DEACTIVATED") {
    return <PermissionDenied title="Action unavailable" message="Your account status prevents access to this section." />;
  }

  if (!canFn(currentUser, perm)) {
    if (inline) return <PermissionDenied />;
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}