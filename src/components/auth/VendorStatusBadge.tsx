import { Badge } from "@/components/ui/badge";
import { BadgeCheck, Clock, ShieldAlert, Store } from "lucide-react";
import type { VendorStatus } from "@/data/mock-users";

const META: Record<VendorStatus, { label: string; className: string; Icon: typeof BadgeCheck } | null> = {
  none:     null,
  pending:  { label: "Application under review", className: "bg-amber-500/15 text-amber-700 border-amber-500/30 dark:text-amber-300", Icon: Clock },
  approved: { label: "Approved seller",          className: "bg-emerald-500/15 text-emerald-700 border-emerald-500/30 dark:text-emerald-300", Icon: BadgeCheck },
  active:   { label: "Verified seller",          className: "bg-emerald-500/15 text-emerald-700 border-emerald-500/30 dark:text-emerald-300", Icon: BadgeCheck },
  rejected: { label: "Application rejected",     className: "bg-red-500/15 text-red-700 border-red-500/30 dark:text-red-300", Icon: ShieldAlert },
};

export default function VendorStatusBadge({ status, className }: { status?: VendorStatus; className?: string }) {
  const meta = status ? META[status] : null;
  if (!meta) return null;
  const { Icon, label, className: variant } = meta;
  return (
    <Badge variant="outline" className={`${variant} gap-1 ${className ?? ""}`}>
      <Icon className="h-3 w-3" />
      {label}
    </Badge>
  );
}

export function VendorVerifiedBadge({ className }: { className?: string }) {
  return (
    <Badge variant="outline" className={`bg-blue-500/15 text-blue-700 border-blue-500/30 dark:text-blue-300 gap-1 ${className ?? ""}`}>
      <Store className="h-3 w-3" /> Verified Store
    </Badge>
  );
}