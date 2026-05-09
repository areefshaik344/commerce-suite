import ProtectedRoute from "./ProtectedRoute";
import type { UserRole } from "@/data/mock-users";

/** Sugar over ProtectedRoute for explicit role gating. */
export default function RoleRoute({ role, children }: { role: UserRole | UserRole[]; children: React.ReactNode }) {
  const roles = Array.isArray(role) ? role : [role];
  return <ProtectedRoute allowedRoles={roles}>{children}</ProtectedRoute>;
}
