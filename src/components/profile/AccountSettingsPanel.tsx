import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Input } from "@/components/ui/input";
import { AlertTriangle, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { useProfile } from "@/hooks/useProfile";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent,
  AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog";

export function AccountSettingsPanel() {
  const { preferences, updatePreferences, profile, deactivate, deleteAccount, savingScope } = useProfile();
  const [confirmDeactivate, setConfirmDeactivate] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deletePassword, setDeletePassword] = useState("");
  const [deleteAck, setDeleteAck] = useState(false);
  const isSaving = savingScope === "lifecycle" || savingScope === "preferences";

  if (!preferences || !profile) return null;

  return (
    <div className="space-y-4">
      <Card className="shadow-card">
        <CardHeader className="pb-3"><CardTitle className="text-base">Appearance & Language</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <Label className="text-xs">Theme</Label>
              <Select value={preferences.theme} onValueChange={(v) => updatePreferences({ theme: v as never }).catch(() => toast.error("Failed"))}>
                <SelectTrigger className="mt-1"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="light">Light</SelectItem>
                  <SelectItem value="dark">Dark</SelectItem>
                  <SelectItem value="system">System default</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label className="text-xs">Language</Label>
              <Select value={preferences.language} onValueChange={(v) => updatePreferences({ language: v as never }).catch(() => toast.error("Failed"))}>
                <SelectTrigger className="mt-1"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="en">English</SelectItem>
                  <SelectItem value="hi">हिन्दी</SelectItem>
                  <SelectItem value="ta">தமிழ்</SelectItem>
                  <SelectItem value="te">తెలుగు</SelectItem>
                  <SelectItem value="bn">বাংলা</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium">Show my profile publicly</p>
              <p className="text-xs text-muted-foreground">Let other shoppers see your reviews and public lists.</p>
            </div>
            <Switch
              checked={preferences.privacyShowProfile}
              onCheckedChange={(v) => updatePreferences({ privacyShowProfile: v }).catch(() => toast.error("Failed"))}
            />
          </div>
        </CardContent>
      </Card>

      <Card className="shadow-card border-destructive/40">
        <CardHeader className="pb-3 flex flex-row items-center gap-2">
          <AlertTriangle className="h-4 w-4 text-destructive" />
          <CardTitle className="text-base">Danger Zone</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between gap-4 p-3 rounded-lg border">
            <div>
              <p className="text-sm font-medium">Deactivate account</p>
              <p className="text-xs text-muted-foreground">Hide your profile and pause all activity. You can reactivate by logging in.</p>
            </div>
            <Button variant="outline" onClick={() => setConfirmDeactivate(true)} disabled={isSaving}>
              Deactivate
            </Button>
          </div>
          <div className="flex items-center justify-between gap-4 p-3 rounded-lg border border-destructive/40 bg-destructive/5">
            <div>
              <p className="text-sm font-medium text-destructive">Delete account</p>
              <p className="text-xs text-muted-foreground">Permanently remove your account and personal data. This can't be undone.</p>
            </div>
            <Button variant="destructive" onClick={() => { setDeletePassword(""); setDeleteAck(false); setDeleteOpen(true); }} disabled={isSaving}>
              Delete account
            </Button>
          </div>
        </CardContent>
      </Card>

      <ConfirmDialog
        open={confirmDeactivate} onOpenChange={setConfirmDeactivate}
        title="Deactivate your account?" description="Your profile will be hidden until you log back in to reactivate."
        confirmLabel="Deactivate" variant="destructive"
        onConfirm={async () => {
          try { await deactivate(); toast.success("Account deactivated"); }
          catch (e) { toast.error(e instanceof Error ? e.message : "Failed"); }
        }}
      />

      <AlertDialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete your account?</AlertDialogTitle>
            <AlertDialogDescription>
              This permanently deletes your profile, orders history visibility, addresses and preferences.
              You will be logged out immediately.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="space-y-3 py-2">
            <div>
              <Label className="text-xs">Confirm with your password</Label>
              <Input type="password" value={deletePassword} onChange={(e) => setDeletePassword(e.target.value)} className="mt-1" autoComplete="current-password" />
            </div>
            <label className="flex items-start gap-2 text-xs">
              <input type="checkbox" checked={deleteAck} onChange={(e) => setDeleteAck(e.target.checked)} className="mt-0.5" />
              <span>I understand this action is permanent and cannot be undone.</span>
            </label>
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              disabled={!deletePassword || !deleteAck || isSaving}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={async (e) => {
                e.preventDefault();
                try { await deleteAccount(deletePassword); toast.success("Account deleted"); setDeleteOpen(false); }
                catch (err) { toast.error(err instanceof Error ? err.message : "Failed"); }
              }}
            >
              {isSaving ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : null}
              Permanently delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}