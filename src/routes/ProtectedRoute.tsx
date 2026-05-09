import { Navigate, useLocation } from "react-router-dom";
import { useAuthStore } from "@/store/authStore";
import type { UserRole } from "@/data/mock-users";

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: UserRole[];
}

/** Requires authentication; optionally restricts to specific roles. */
export default function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const { isAuthenticated, currentRole, isBootstrapping } = useAuthStore();
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

  return <>{children}</>;
}
