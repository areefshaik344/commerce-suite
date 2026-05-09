import { Navigate, useLocation } from "react-router-dom";
import { useAuthStore } from "@/store/authStore";

const ROLE_HOME: Record<string, string> = {
  admin: "/admin",
  vendor: "/vendor",
  customer: "/",
};

/** Bounces already-authenticated users away from auth pages. */
export default function PublicRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, currentRole } = useAuthStore();
  const location = useLocation();

  if (isAuthenticated) {
    const from = (location.state as { from?: string } | null)?.from;
    return <Navigate to={from || ROLE_HOME[currentRole] || "/"} replace />;
  }
  return <>{children}</>;
}
