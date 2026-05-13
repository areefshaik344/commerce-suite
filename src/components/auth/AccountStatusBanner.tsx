import { AlertTriangle, Info, ShieldAlert, CheckCircle2 } from "lucide-react";
import { Link } from "react-router-dom";
import { usePermissions } from "@/hooks/usePermissions";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

const VARIANT_STYLES = {
  info:        "bg-blue-50 border-blue-200 text-blue-900 dark:bg-blue-950/40 dark:border-blue-900 dark:text-blue-100",
  warning:     "bg-amber-50 border-amber-200 text-amber-900 dark:bg-amber-950/40 dark:border-amber-900 dark:text-amber-100",
  destructive: "bg-red-50 border-red-200 text-red-900 dark:bg-red-950/40 dark:border-red-900 dark:text-red-100",
  success:     "bg-emerald-50 border-emerald-200 text-emerald-900 dark:bg-emerald-950/40 dark:border-emerald-900 dark:text-emerald-100",
} as const;

const ICONS = { info: Info, warning: AlertTriangle, destructive: ShieldAlert, success: CheckCircle2 } as const;

/** Global account-state banner. Renders nothing for ACTIVE users. */
export default function AccountStatusBanner({ className }: { className?: string }) {
  const { banner } = usePermissions();
  if (!banner.show) return null;
  const Icon = ICONS[banner.variant];
  return (
    <div role="status" className={cn("border-b", VARIANT_STYLES[banner.variant], className)}>
      <div className="container flex flex-wrap items-center gap-3 py-2.5 text-sm">
        <Icon className="h-4 w-4 shrink-0" />
        <div className="flex-1 min-w-[200px]">
          <span className="font-semibold">{banner.title}.</span>{" "}
          <span className="opacity-90">{banner.message}</span>
        </div>
        {banner.actionLabel && banner.actionHref && (
          <Button asChild size="sm" variant="outline" className="bg-background/60">
            <Link to={banner.actionHref}>{banner.actionLabel}</Link>
          </Button>
        )}
      </div>
    </div>
  );
}