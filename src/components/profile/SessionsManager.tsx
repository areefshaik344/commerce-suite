import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Laptop, Smartphone, Monitor, LogOut, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { useProfile } from "@/hooks/useProfile";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { formatDistanceToNowStrict } from "date-fns";
import type { DeviceSession } from "@/data/mock-users";

function deviceIcon(s: DeviceSession) {
  const d = s.device.toLowerCase();
  if (d.includes("iphone") || d.includes("android") || d.includes("phone")) return Smartphone;
  if (d.includes("mac")) return Laptop;
  return Monitor;
}

export function SessionsManager() {
  const { sessions, isLoading, savingScope, revokeSession, revokeAllOtherSessions } = useProfile();
  const [confirmRevoke, setConfirmRevoke] = useState<string | null>(null);
  const [confirmAll, setConfirmAll] = useState(false);
  const isSaving = savingScope === "sessions";

  const others = sessions.filter((s) => !s.current).length;

  return (
    <Card className="shadow-card">
      <CardHeader className="pb-3 flex flex-row items-center justify-between">
        <CardTitle className="text-base">Active Sessions</CardTitle>
        {others > 0 && (
          <Button size="sm" variant="outline" className="gap-1 text-xs" onClick={() => setConfirmAll(true)} disabled={isSaving}>
            <LogOut className="h-3 w-3" />Sign out all other devices
          </Button>
        )}
      </CardHeader>
      <CardContent className="space-y-3">
        {isLoading
          ? [1, 2].map((i) => <Skeleton key={i} className="h-16 w-full" />)
          : sessions.map((s) => {
              const Icon = deviceIcon(s);
              return (
                <div key={s.id} className="flex items-center justify-between gap-3 p-3 rounded-lg border bg-card">
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-full bg-muted grid place-items-center">
                      <Icon className="h-5 w-5 text-muted-foreground" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-medium">{s.device} · {s.browser}</p>
                        {s.current && <Badge className="text-[10px] bg-success/10 text-success border-0">This device</Badge>}
                      </div>
                      <p className="text-xs text-muted-foreground">
                        {s.os} · {s.location} · {s.ip}
                      </p>
                      <p className="text-[11px] text-muted-foreground">
                        Last active {formatDistanceToNowStrict(new Date(s.lastActiveAt))} ago
                      </p>
                    </div>
                  </div>
                  {!s.current && (
                    <Button size="sm" variant="ghost" className="text-destructive text-xs" onClick={() => setConfirmRevoke(s.id)} disabled={isSaving}>
                      Sign out
                    </Button>
                  )}
                </div>
              );
            })}
        {isSaving && (
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Loader2 className="h-3 w-3 animate-spin" />Updating sessions…
          </div>
        )}
      </CardContent>

      <ConfirmDialog
        open={!!confirmRevoke} onOpenChange={(v) => !v && setConfirmRevoke(null)}
        title="Sign out this device?" description="That device will need to log in again to use the account."
        confirmLabel="Sign out" variant="destructive"
        onConfirm={async () => {
          if (!confirmRevoke) return;
          try { await revokeSession(confirmRevoke); toast.success("Device signed out"); }
          catch (e) { toast.error(e instanceof Error ? e.message : "Could not sign out"); }
          finally { setConfirmRevoke(null); }
        }}
      />
      <ConfirmDialog
        open={confirmAll} onOpenChange={setConfirmAll}
        title="Sign out all other devices?" description="You'll stay logged in here. All other sessions will be revoked immediately."
        confirmLabel="Sign out all" variant="destructive"
        onConfirm={async () => {
          try { await revokeAllOtherSessions(); toast.success("All other devices signed out"); }
          catch (e) { toast.error(e instanceof Error ? e.message : "Failed"); }
          finally { setConfirmAll(false); }
        }}
      />
    </Card>
  );
}