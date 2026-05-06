import { useStore, VendorApplication } from "@/store/useStore";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Check, X, Clock, Store, Eye, FileText, ShieldCheck } from "lucide-react";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { useState } from "react";
import { toast } from "@/hooks/use-toast";
import { ONBOARDING_STAGES, useVendorOnboardingStore, OnboardingStageId } from "@/store/vendorOnboardingStore";
import { cn } from "@/lib/utils";

const statusColors: Record<string, string> = {
  pending: "bg-warning/10 text-warning",
  approved: "bg-success/10 text-success",
  rejected: "bg-destructive/10 text-destructive",
};

export default function AdminVendorApplications() {
  const { vendorApplications, approveVendor, rejectVendor } = useStore();
  const onboarding = useVendorOnboardingStore();
  const [approveTarget, setApproveTarget] = useState<VendorApplication | null>(null);
  const [rejectTarget, setRejectTarget] = useState<VendorApplication | null>(null);
  const [tab, setTab] = useState<"pending" | "all" | "onboarding">("pending");
  const [reviewOpen, setReviewOpen] = useState(false);
  const [rejectStage, setRejectStage] = useState<OnboardingStageId | null>(null);
  const [rejectNote, setRejectNote] = useState("");

  const filtered = tab === "pending" ? vendorApplications.filter(a => a.status === "pending") : vendorApplications;

  return (
    <div className="space-y-4">
      <div>
        <h1 className="font-display text-xl font-bold">Vendor Applications</h1>
        <p className="text-sm text-muted-foreground">
          {vendorApplications.filter(a => a.status === "pending").length} pending review
        </p>
      </div>

      <div className="flex gap-2">
        <Button variant={tab === "pending" ? "default" : "outline"} size="sm" onClick={() => setTab("pending")}>
          Pending ({vendorApplications.filter(a => a.status === "pending").length})
        </Button>
        <Button variant={tab === "all" ? "default" : "outline"} size="sm" onClick={() => setTab("all")}>
          All ({vendorApplications.length})
        </Button>
        <Button variant={tab === "onboarding" ? "default" : "outline"} size="sm" onClick={() => setTab("onboarding")}>
          Onboarding queue
        </Button>
      </div>

      {tab === "onboarding" ? (
        <Card className="shadow-card">
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <ShieldCheck className="h-4 w-4 text-primary" />
              Live onboarding ({onboarding.finalStatus.replace("_", " ")})
            </CardTitle>
            <p className="text-xs text-muted-foreground">{onboarding.business.businessName || "Unnamed seller"} · {onboarding.completionPercent()}% complete</p>
          </CardHeader>
          <CardContent className="space-y-3">
            {ONBOARDING_STAGES.slice(0, -1).map((s) => {
              const st = onboarding.stageStatus[s.id];
              return (
                <div key={s.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div className="flex items-center gap-2">
                    <FileText className="h-4 w-4 text-muted-foreground" />
                    <span className="text-sm font-medium">{s.title}</span>
                    <Badge className={cn(
                      "text-xs border-0 capitalize",
                      st === "verified" ? "bg-success/10 text-success" :
                      st === "submitted" ? "bg-primary/10 text-primary" :
                      st === "rejected" ? "bg-destructive/10 text-destructive" :
                      st === "in_progress" ? "bg-warning/10 text-warning" : "bg-muted text-muted-foreground"
                    )}>{st.replace("_", " ")}</Badge>
                  </div>
                  {st === "submitted" && (
                    <div className="flex gap-2">
                      <Button size="sm" variant="outline" className="text-success border-success/30" onClick={() => { onboarding.verifyStage(s.id); toast({ title: `${s.title} verified` }); }}>
                        <Check className="h-3.5 w-3.5" />
                      </Button>
                      <Button size="sm" variant="outline" className="text-destructive border-destructive/30" onClick={() => { setRejectStage(s.id); setRejectNote(""); }}>
                        <X className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  )}
                </div>
              );
            })}
            {onboarding.finalStatus === "under_review" && (
              <div className="flex gap-2 pt-2 border-t">
                <Button size="sm" className="flex-1" onClick={() => { onboarding.approveOnboarding(); toast({ title: "Seller approved & live!" }); }}>
                  <Check className="h-4 w-4 mr-1" /> Approve seller (final)
                </Button>
                <Button size="sm" variant="outline" className="text-destructive border-destructive/30" onClick={() => { onboarding.rejectOnboarding("Application not meeting platform standards"); toast({ title: "Application rejected" }); }}>
                  <X className="h-4 w-4 mr-1" /> Reject
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      ) : filtered.length === 0 ? (
        <Card className="shadow-card">
          <CardContent className="p-8 text-center text-muted-foreground">
            <Store className="h-8 w-8 mx-auto mb-2 opacity-50" />
            <p className="text-sm">No {tab === "pending" ? "pending " : ""}applications</p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {filtered.map(app => (
            <Card key={app.id} className="shadow-card">
              <CardContent className="p-4">
                <div className="flex items-start justify-between">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <h3 className="font-display font-semibold">{app.storeName}</h3>
                      <Badge variant="secondary" className={`text-xs capitalize border-0 ${statusColors[app.status]}`}>
                        {app.status}
                      </Badge>
                    </div>
                    <p className="text-sm text-muted-foreground">{app.description}</p>
                    <div className="flex flex-wrap gap-4 text-xs text-muted-foreground mt-2">
                      <span>👤 {app.name}</span>
                      <span>📧 {app.email}</span>
                      <span>📞 {app.phone}</span>
                      <span>📂 {app.category}</span>
                      <span><Clock className="h-3 w-3 inline mr-1" />Applied {app.appliedDate}</span>
                    </div>
                  </div>
                  {app.status === "pending" && (
                    <div className="flex gap-2 shrink-0">
                      <Button size="sm" variant="ghost" className="gap-1" onClick={() => setReviewOpen(true)}>
                        <Eye className="h-3.5 w-3.5" /> Review
                      </Button>
                      <Button size="sm" variant="outline" className="gap-1 text-success border-success/30 hover:bg-success/10" onClick={() => setApproveTarget(app)}>
                        <Check className="h-3.5 w-3.5" /> Approve
                      </Button>
                      <Button size="sm" variant="outline" className="gap-1 text-destructive border-destructive/30 hover:bg-destructive/10" onClick={() => setRejectTarget(app)}>
                        <X className="h-3.5 w-3.5" /> Reject
                      </Button>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={!!approveTarget}
        onOpenChange={() => setApproveTarget(null)}
        title="Approve Application"
        description={`Approve "${approveTarget?.storeName}" by ${approveTarget?.name}? They will be able to start listing products.`}
        confirmLabel="Approve"
        variant="default"
        onConfirm={() => { approveVendor(approveTarget!.id); toast({ title: "Application approved" }); setApproveTarget(null); }}
      />
      <ConfirmDialog
        open={!!rejectTarget}
        onOpenChange={() => setRejectTarget(null)}
        title="Reject Application"
        description={`Reject "${rejectTarget?.storeName}" by ${rejectTarget?.name}? They will be notified via email.`}
        confirmLabel="Reject"
        onConfirm={() => { rejectVendor(rejectTarget!.id); toast({ title: "Application rejected" }); setRejectTarget(null); }}
      />

      <Dialog open={!!rejectStage} onOpenChange={() => setRejectStage(null)}>
        <DialogContent>
          <DialogHeader><DialogTitle>Reject stage</DialogTitle></DialogHeader>
          <div className="space-y-3">
            <p className="text-sm text-muted-foreground">Provide a clear reason. The seller will see this note and can re-submit.</p>
            <Textarea rows={4} value={rejectNote} onChange={(e) => setRejectNote(e.target.value)} placeholder="e.g. PAN card image is blurred, please re-upload a clear scan." />
            <Button className="w-full" onClick={() => {
              if (rejectStage && rejectNote.trim()) {
                onboarding.rejectStage(rejectStage, rejectNote.trim());
                toast({ title: "Stage rejected", description: "Seller has been notified." });
                setRejectStage(null);
              }
            }}>Submit rejection</Button>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog open={reviewOpen} onOpenChange={setReviewOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Application review checklist</DialogTitle></DialogHeader>
          <div className="space-y-2 text-sm">
            {["Identity & PAN match", "GST validity (if applicable)", "Bank penny-drop verified", "Pickup address reachable", "Category fit"].map((c) => (
              <div key={c} className="flex items-center gap-2 rounded-md border p-2"><Check className="h-3.5 w-3.5 text-success" />{c}</div>
            ))}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
