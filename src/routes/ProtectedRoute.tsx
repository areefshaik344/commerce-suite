import { Navigate, useLocation } from "react-router-dom";
import { useAuthStore } from "@/store/authStore";
import type { UserRole } from "@/data/mock-users";
import { resolveAccountStatus } from "@/lib/accountStatus";
import PermissionDenied from "@/components/auth/PermissionDenied";

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: UserRole[];
  /**
   * Account-status policy:
   *  - "strict" (default): SUSPENDED / BANNED / DEACTIVATED users see <PermissionDenied/>.
   *  - "allow-restricted": status is enforced by feature gates further down,
   *    not by the route. Use for /profile so users can reactivate / appeal.
   */
  statusPolicy?: "strict" | "allow-restricted";
}

/** Requires authentication; optionally restricts to specific roles. */
export default function ProtectedRoute({ children, allowedRoles, statusPolicy = "strict" }: ProtectedRouteProps) {
  const { isAuthenticated, currentRole, currentUser, isBootstrapping } = useAuthStore();
  const location = useLocation();

  if (isBootstrapping) return null;

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(currentRole)) {
    if (currentRole === "admin") return <Navigate to="/admin" replace />;
    if (currentRole === "vendor") return <Navigate to="/vendor" replace />;
    return <Navigate to="/" replace />;
  }

  if (statusPolicy === "strict") {
    const status = resolveAccountStatus(currentUser);
    if (status === "SUSPENDED" || status === "BANNED" || status === "DEACTIVATED") {
      return (
        <PermissionDenied
          title={status === "BANNED" ? "Account banned" : status === "SUSPENDED" ? "Account suspended" : "Account deactivated"}
          message="Your account status prevents access to this section. Visit your profile or contact support to resolve this."
          homeHref="/profile"
        />
      );
    }
  }

  return <>{children}</>;
}
