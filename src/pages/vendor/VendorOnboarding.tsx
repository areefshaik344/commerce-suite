import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Check, Upload, FileText, Building2, CreditCard, ShieldCheck, ChevronRight, ChevronLeft, AlertCircle, MapPin, PenTool, PackagePlus, Rocket, Mail, Phone, Clock, XCircle, CheckCircle2, Sparkles } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { ONBOARDING_STAGES, OnboardingStageId, useVendorOnboardingStore, StageStatus } from "@/store/vendorOnboardingStore";
import { cn } from "@/lib/utils";

const stageIcons: Record<OnboardingStageId, React.ComponentType<{ className?: string }>> = {
  account: Mail,
  business: Building2,
  tax: FileText,
  pickup: MapPin,
  bank: CreditCard,
  signature: PenTool,
  catalog: PackagePlus,
  golive: Rocket,
};

const statusBadge: Record<StageStatus, { label: string; className: string }> = {
  not_started: { label: "Not started", className: "bg-muted text-muted-foreground" },
  in_progress: { label: "In progress", className: "bg-warning/10 text-warning" },
  submitted: { label: "Under review", className: "bg-primary/10 text-primary" },
  verified: { label: "Verified", className: "bg-success/10 text-success" },
  rejected: { label: "Action required", className: "bg-destructive/10 text-destructive" },
};

function UploadBox({ label, hint, uploaded, onUpload }: { label: string; hint?: string; uploaded: boolean; onUpload: () => void }) {
  return (
    <div
      onClick={() => !uploaded && onUpload()}
      className={cn(
        "flex items-center gap-3 rounded-lg border-2 border-dashed p-4 cursor-pointer transition-colors",
        uploaded ? "border-primary/30 bg-primary/5" : "border-border hover:border-primary/50"
      )}
    >
      <div className={cn("h-10 w-10 rounded-lg flex items-center justify-center shrink-0", uploaded ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground")}>
        {uploaded ? <Check className="h-5 w-5" /> : <Upload className="h-5 w-5" />}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs text-muted-foreground">{uploaded ? "Document uploaded · Click trash to replace" : hint || "Click to upload (PDF, JPG, PNG · max 5MB)"}</p>
      </div>
      {uploaded && <Badge className="bg-primary/10 text-primary border-0">Uploaded</Badge>}
    </div>
  );
}

export default function VendorOnboarding() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const store = useVendorOnboardingStore();
  const [otpEmail, setOtpEmail] = useState("");
  const [otpPhone, setOtpPhone] = useState("");

  const stageIdx = ONBOARDING_STAGES.findIndex((s) => s.id === store.currentStage);
  const meta = ONBOARDING_STAGES[stageIdx];
  const status = store.stageStatus[meta.id];
  const completion = store.completionPercent();

  const verifiedCount = useMemo(
    () => ONBOARDING_STAGES.filter((s) => store.stageStatus[s.id] === "verified").length,
    [store.stageStatus]
  );
  const allReady = useMemo(
    () => ONBOARDING_STAGES.slice(0, -1).every((s) => ["submitted", "verified"].includes(store.stageStatus[s.id])),
    [store.stageStatus]
  );

  const goNext = () => {
    if (stageIdx < ONBOARDING_STAGES.length - 1) store.setStage(ONBOARDING_STAGES[stageIdx + 1].id);
  };
  const goPrev = () => {
    if (stageIdx > 0) store.setStage(ONBOARDING_STAGES[stageIdx - 1].id);
  };

  const handleSubmitStage = () => {
    store.submitStage(meta.id);
    toast({ title: "Submitted for verification", description: `${meta.title} is now under review.` });
    goNext();
  };

  const handleSimulateVerify = () => {
    store.verifyStage(meta.id);
    toast({ title: "Verified ✓", description: `${meta.title} approved (simulated).` });
  };

  const handleFinalSubmit = () => {
    store.submitForReview();
    toast({ title: "Application submitted!", description: "Our team will review within 24-48 hours." });
  };

  const handleSimulateGoLive = () => {
    store.approveOnboarding();
    toast({ title: "🎉 You're live!", description: "Start managing your seller dashboard." });
    navigate("/vendor");
  };

  const renderStageBody = () => {
    switch (meta.id) {
      case "account":
        return (
          <div className="space-y-4">
            <div className="rounded-lg border p-4 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Mail className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm font-medium">Email verification</span>
                  {store.account.emailVerified && <Badge className="bg-success/10 text-success border-0">Verified</Badge>}
                </div>
                {!store.account.emailVerified && (
                  <Button size="sm" variant="outline" onClick={() => { store.updateAccount({ emailVerified: true }); toast({ title: "Email verified" }); }}>
                    Send OTP
                  </Button>
                )}
              </div>
              {!store.account.emailVerified && (
                <Input placeholder="Enter 6-digit OTP" value={otpEmail} onChange={(e) => setOtpEmail(e.target.value)} maxLength={6} />
              )}
            </div>
            <div className="rounded-lg border p-4 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Phone className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm font-medium">Phone verification</span>
                  {store.account.phoneVerified && <Badge className="bg-success/10 text-success border-0">Verified</Badge>}
                </div>
                {!store.account.phoneVerified && (
                  <Button size="sm" variant="outline" onClick={() => { store.updateAccount({ phoneVerified: true }); toast({ title: "Phone verified" }); }}>
                    Send OTP
                  </Button>
                )}
              </div>
              {!store.account.phoneVerified && (
                <Input placeholder="Enter 6-digit OTP" value={otpPhone} onChange={(e) => setOtpPhone(e.target.value)} maxLength={6} />
              )}
            </div>
          </div>
        );

      case "business":
        return (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Display Store Name *</Label>
                <Input value={store.business.businessName} onChange={(e) => store.updateBusiness({ businessName: e.target.value })} placeholder="e.g. TechZone Electronics" />
              </div>
              <div className="space-y-2">
                <Label>Legal Entity Name *</Label>
                <Input value={store.business.legalName} onChange={(e) => store.updateBusiness({ legalName: e.target.value })} placeholder="As per GST/PAN" />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Business Type *</Label>
                <Select value={store.business.businessType} onValueChange={(v) => store.updateBusiness({ businessType: v })}>
                  <SelectTrigger><SelectValue placeholder="Select type" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="individual">Individual / Sole Proprietor</SelectItem>
                    <SelectItem value="partnership">Partnership</SelectItem>
                    <SelectItem value="pvtltd">Private Limited</SelectItem>
                    <SelectItem value="llp">LLP</SelectItem>
                    <SelectItem value="huf">HUF</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Primary Category *</Label>
                <Select value={store.business.category} onValueChange={(v) => store.updateBusiness({ category: v })}>
                  <SelectTrigger><SelectValue placeholder="Select category" /></SelectTrigger>
                  <SelectContent>
                    {["Electronics", "Fashion", "Home & Living", "Beauty", "Sports", "Books", "Groceries", "Toys & Games"].map((c) => (
                      <SelectItem key={c} value={c.toLowerCase()}>{c}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Year Established</Label>
                <Input value={store.business.yearEstablished} onChange={(e) => store.updateBusiness({ yearEstablished: e.target.value })} placeholder="2020" maxLength={4} />
              </div>
              <div className="space-y-2">
                <Label>Website (optional)</Label>
                <Input value={store.business.website} onChange={(e) => store.updateBusiness({ website: e.target.value })} placeholder="https://example.com" />
              </div>
            </div>
            <div className="space-y-2">
              <Label>Store Description</Label>
              <Textarea rows={3} value={store.business.description} onChange={(e) => store.updateBusiness({ description: e.target.value })} placeholder="What kind of products do you sell?" />
            </div>
          </div>
        );

      case "tax":
        return (
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Are you GST registered? *</Label>
              <RadioGroup
                value={store.tax.hasGst}
                onValueChange={(v) => store.updateTax({ hasGst: v as "yes" | "no" })}
                className="flex gap-6"
              >
                <div className="flex items-center gap-2"><RadioGroupItem value="yes" id="gst-y" /><Label htmlFor="gst-y" className="font-normal">Yes, I have GSTIN</Label></div>
                <div className="flex items-center gap-2"><RadioGroupItem value="no" id="gst-n" /><Label htmlFor="gst-n" className="font-normal">No (only allowed for GST-exempt categories)</Label></div>
              </RadioGroup>
            </div>
            {store.tax.hasGst === "yes" && (
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>GSTIN *</Label>
                    <Input value={store.tax.gstNumber} onChange={(e) => store.updateTax({ gstNumber: e.target.value.toUpperCase() })} placeholder="22AAAAA0000A1Z5" maxLength={15} />
                  </div>
                  <div className="space-y-2">
                    <Label>PAN Number *</Label>
                    <Input value={store.tax.panNumber} onChange={(e) => store.updateTax({ panNumber: e.target.value.toUpperCase() })} placeholder="ABCDE1234F" maxLength={10} />
                  </div>
                </div>
                <UploadBox label="GST Certificate" uploaded={store.tax.gstDocUploaded} onUpload={() => store.updateTax({ gstDocUploaded: true })} />
              </div>
            )}
            {store.tax.hasGst === "no" && (
              <div className="space-y-2">
                <Label>PAN Number *</Label>
                <Input value={store.tax.panNumber} onChange={(e) => store.updateTax({ panNumber: e.target.value.toUpperCase() })} placeholder="ABCDE1234F" maxLength={10} />
              </div>
            )}
            <UploadBox label="PAN Card" uploaded={store.tax.panDocUploaded} onUpload={() => store.updateTax({ panDocUploaded: true })} />
          </div>
        );

      case "pickup":
        return (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Contact Name *</Label><Input value={store.pickup.contactName} onChange={(e) => store.updatePickup({ contactName: e.target.value })} /></div>
              <div className="space-y-2"><Label>Phone *</Label><Input value={store.pickup.phone} onChange={(e) => store.updatePickup({ phone: e.target.value })} placeholder="+91 98765 43210" /></div>
            </div>
            <div className="space-y-2"><Label>Address Line 1 *</Label><Input value={store.pickup.addressLine1} onChange={(e) => store.updatePickup({ addressLine1: e.target.value })} placeholder="Building, street" /></div>
            <div className="space-y-2"><Label>Address Line 2</Label><Input value={store.pickup.addressLine2} onChange={(e) => store.updatePickup({ addressLine2: e.target.value })} placeholder="Area, locality" /></div>
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-2"><Label>City *</Label><Input value={store.pickup.city} onChange={(e) => store.updatePickup({ city: e.target.value })} /></div>
              <div className="space-y-2"><Label>State *</Label><Input value={store.pickup.state} onChange={(e) => store.updatePickup({ state: e.target.value })} /></div>
              <div className="space-y-2"><Label>Pincode *</Label><Input value={store.pickup.pincode} onChange={(e) => store.updatePickup({ pincode: e.target.value })} maxLength={6} /></div>
            </div>
            <div className="space-y-2"><Label>Landmark</Label><Input value={store.pickup.landmark} onChange={(e) => store.updatePickup({ landmark: e.target.value })} placeholder="Near metro station" /></div>
          </div>
        );

      case "bank": {
        const acctMismatch = store.bank.confirmAccountNumber && store.bank.accountNumber !== store.bank.confirmAccountNumber;
        return (
          <div className="space-y-4">
            <div className="space-y-2"><Label>Account Holder Name *</Label><Input value={store.bank.accountHolder} onChange={(e) => store.updateBank({ accountHolder: e.target.value })} placeholder="As per bank records" /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Account Number *</Label><Input value={store.bank.accountNumber} onChange={(e) => store.updateBank({ accountNumber: e.target.value })} /></div>
              <div className="space-y-2"><Label>Re-enter Account Number *</Label><Input value={store.bank.confirmAccountNumber} onChange={(e) => store.updateBank({ confirmAccountNumber: e.target.value })} />{acctMismatch && <p className="text-xs text-destructive">Account numbers don't match</p>}</div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>IFSC Code *</Label><Input value={store.bank.ifscCode} onChange={(e) => store.updateBank({ ifscCode: e.target.value.toUpperCase() })} maxLength={11} /></div>
              <div className="space-y-2">
                <Label>Account Type *</Label>
                <Select value={store.bank.accountType} onValueChange={(v) => store.updateBank({ accountType: v as "savings" | "current" })}>
                  <SelectTrigger><SelectValue placeholder="Select" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="savings">Savings</SelectItem>
                    <SelectItem value="current">Current</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Bank Name</Label><Input value={store.bank.bankName} onChange={(e) => store.updateBank({ bankName: e.target.value })} placeholder="HDFC Bank" /></div>
              <div className="space-y-2"><Label>Branch</Label><Input value={store.bank.branch} onChange={(e) => store.updateBank({ branch: e.target.value })} placeholder="Andheri West" /></div>
            </div>
            <UploadBox label="Cancelled Cheque or Bank Statement" uploaded={store.bank.chequeUploaded} onUpload={() => store.updateBank({ chequeUploaded: true })} />
            <div className="rounded-lg border p-4 flex items-center justify-between">
              <div>
                <p className="text-sm font-medium">Penny-drop verification</p>
                <p className="text-xs text-muted-foreground">We'll deposit ₹1 to confirm your account is valid</p>
              </div>
              {store.bank.pennyDropVerified ? (
                <Badge className="bg-success/10 text-success border-0 gap-1"><CheckCircle2 className="h-3 w-3" /> Verified</Badge>
              ) : (
                <Button size="sm" variant="outline" onClick={() => { store.updateBank({ pennyDropVerified: true }); toast({ title: "Penny-drop verified" }); }} disabled={!store.bank.accountNumber || !store.bank.ifscCode}>
                  Run check
                </Button>
              )}
            </div>
          </div>
        );
      }

      case "signature":
        return (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Authorized Signatory Name *</Label><Input value={store.signature.authorizedSignatory} onChange={(e) => store.updateSignature({ authorizedSignatory: e.target.value })} /></div>
              <div className="space-y-2"><Label>Designation</Label><Input value={store.signature.designation} onChange={(e) => store.updateSignature({ designation: e.target.value })} placeholder="Founder / Director" /></div>
            </div>
            <UploadBox label="Signature image" hint="Upload a clear scan/photo of your signature on white paper" uploaded={store.signature.signatureUploaded} onUpload={() => store.updateSignature({ signatureUploaded: true })} />
            <Separator />
            <div className="rounded-lg border bg-muted/30 p-4 max-h-48 overflow-auto text-xs text-muted-foreground space-y-2">
              <p className="font-medium text-foreground">MarketHub Seller Agreement</p>
              <p>By accepting, you agree to abide by MarketHub's selling policies, commission structure, return & refund norms, and quality standards.</p>
              <p>You confirm that all uploaded documents are authentic and you are authorized to operate this business.</p>
              <p>MarketHub reserves the right to suspend or terminate your account in case of policy violations.</p>
            </div>
            <div className="flex items-start gap-2">
              <Checkbox id="agree" checked={store.signature.agreementAccepted} onCheckedChange={(v) => store.updateSignature({ agreementAccepted: v === true })} />
              <label htmlFor="agree" className="text-sm leading-relaxed cursor-pointer">
                I have read and agree to the <span className="text-primary">Seller Agreement</span> and <span className="text-primary">Commission Policy</span>.
              </label>
            </div>
          </div>
        );

      case "catalog":
        return (
          <div className="space-y-4">
            <div className="rounded-lg bg-accent/40 p-4 flex items-start gap-3">
              <Sparkles className="h-5 w-5 text-primary mt-0.5 shrink-0" />
              <div className="text-sm">
                <p className="font-medium">Add at least 1 product to go live</p>
                <p className="text-muted-foreground text-xs mt-0.5">You can use bulk upload (CSV) or add products one by one.</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <Button variant="outline" className="h-auto p-4 flex-col items-start gap-1" onClick={() => navigate("/vendor/products/new")}>
                <PackagePlus className="h-5 w-5" />
                <span className="font-medium text-sm">Add single product</span>
                <span className="text-xs text-muted-foreground">Recommended for first-time sellers</span>
              </Button>
              <Button variant="outline" className="h-auto p-4 flex-col items-start gap-1" onClick={() => navigate("/vendor/products/bulk-upload")}>
                <Upload className="h-5 w-5" />
                <span className="font-medium text-sm">Bulk upload (CSV)</span>
                <span className="text-xs text-muted-foreground">Upload many products at once</span>
              </Button>
            </div>
            <Separator />
            <div className="space-y-2">
              <Label>Shipping preference *</Label>
              <RadioGroup value={store.catalog.shippingProvider} onValueChange={(v) => store.updateCatalog({ shippingProvider: v as "self" | "platform" })} className="space-y-2">
                <div className="flex items-start gap-2 rounded-lg border p-3"><RadioGroupItem value="platform" id="ship-p" className="mt-0.5" /><div><Label htmlFor="ship-p" className="font-medium">MarketHub Logistics (recommended)</Label><p className="text-xs text-muted-foreground">We pick up, pack and deliver. Lower returns, faster shipping.</p></div></div>
                <div className="flex items-start gap-2 rounded-lg border p-3"><RadioGroupItem value="self" id="ship-s" className="mt-0.5" /><div><Label htmlFor="ship-s" className="font-medium">Self ship</Label><p className="text-xs text-muted-foreground">You handle packing & shipping using your own courier.</p></div></div>
              </RadioGroup>
            </div>
            <div className="flex items-center gap-2">
              <Checkbox id="wh" checked={store.catalog.primaryWarehouseConfirmed} onCheckedChange={(v) => store.updateCatalog({ primaryWarehouseConfirmed: v === true })} />
              <label htmlFor="wh" className="text-sm cursor-pointer">My pickup address is also my primary warehouse</label>
            </div>
            <div className="rounded-lg border p-3 flex items-center justify-between text-sm">
              <span>Products added so far</span>
              <Badge variant="secondary">{store.catalog.productsAdded}</Badge>
              <Button size="sm" variant="ghost" onClick={() => store.updateCatalog({ productsAdded: store.catalog.productsAdded + 1 })}>+1 (simulate)</Button>
            </div>
          </div>
        );

      case "golive":
        return (
          <div className="space-y-4">
            <div className="rounded-lg border p-4 space-y-3">
              <p className="text-sm font-medium">Verification summary</p>
              <div className="space-y-2">
                {ONBOARDING_STAGES.slice(0, -1).map((s) => {
                  const st = store.stageStatus[s.id];
                  const Icon = stageIcons[s.id];
                  return (
                    <div key={s.id} className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2"><Icon className="h-4 w-4 text-muted-foreground" />{s.title}</div>
                      <Badge className={cn("border-0 text-xs", statusBadge[st].className)}>{statusBadge[st].label}</Badge>
                    </div>
                  );
                })}
              </div>
            </div>
            {store.finalStatus === "draft" && (
              <Button className="w-full gap-2" onClick={handleFinalSubmit} disabled={!allReady}>
                <Rocket className="h-4 w-4" /> Submit for final review
              </Button>
            )}
            {store.finalStatus === "under_review" && (
              <div className="rounded-lg bg-primary/5 border border-primary/20 p-4 text-center space-y-3">
                <Clock className="h-8 w-8 text-primary mx-auto" />
                <div>
                  <p className="font-semibold">Application under review</p>
                  <p className="text-xs text-muted-foreground mt-1">Typical turnaround: 24-48 hours</p>
                </div>
                <Button variant="outline" size="sm" onClick={handleSimulateGoLive}>Simulate approval</Button>
              </div>
            )}
            {store.finalStatus === "approved" && (
              <div className="rounded-lg bg-success/5 border border-success/20 p-6 text-center space-y-2">
                <CheckCircle2 className="h-10 w-10 text-success mx-auto" />
                <p className="font-semibold text-lg">You're approved!</p>
                <p className="text-sm text-muted-foreground">Start managing orders, ads & growth.</p>
                <Button onClick={() => navigate("/vendor")}>Go to dashboard</Button>
              </div>
            )}
            {store.finalStatus === "rejected" && (
              <div className="rounded-lg bg-destructive/5 border border-destructive/20 p-4 space-y-2">
                <XCircle className="h-6 w-6 text-destructive" />
                <p className="font-medium text-sm">Application rejected</p>
                <p className="text-xs text-muted-foreground">Please review the timeline notes and re-submit.</p>
              </div>
            )}
          </div>
        );
    }
  };

  const StageIcon = stageIcons[meta.id];

  return (
    <div className="space-y-6 max-w-6xl">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold">Become a MarketHub Seller</h1>
          <p className="text-sm text-muted-foreground">Complete the steps below to start selling. You can save & resume any time.</p>
        </div>
        <div className="flex items-center gap-3">
          <Badge className="bg-primary/10 text-primary border-0">{verifiedCount}/{ONBOARDING_STAGES.length} verified</Badge>
          <Badge variant="outline" className="capitalize">{store.finalStatus.replace("_", " ")}</Badge>
        </div>
      </div>

      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>Overall progress</span>
          <span>{completion}%</span>
        </div>
        <Progress value={completion} className="h-2" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[260px_1fr_280px] gap-6">
        {/* Stepper */}
        <Card className="shadow-card h-fit">
          <CardContent className="p-3">
            <div className="space-y-1">
              {ONBOARDING_STAGES.map((s, i) => {
                const st = store.stageStatus[s.id];
                const Icon = stageIcons[s.id];
                const active = s.id === meta.id;
                return (
                  <button
                    key={s.id}
                    onClick={() => store.setStage(s.id)}
                    className={cn(
                      "w-full flex items-center gap-3 p-2.5 rounded-lg text-left transition-colors",
                      active ? "bg-primary/10" : "hover:bg-muted/60"
                    )}
                  >
                    <div className={cn(
                      "h-8 w-8 rounded-lg flex items-center justify-center shrink-0",
                      st === "verified" ? "bg-success/15 text-success" :
                      st === "submitted" ? "bg-primary/15 text-primary" :
                      st === "rejected" ? "bg-destructive/15 text-destructive" :
                      st === "in_progress" ? "bg-warning/15 text-warning" :
                      "bg-muted text-muted-foreground"
                    )}>
                      {st === "verified" ? <Check className="h-4 w-4" /> : <Icon className="h-4 w-4" />}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className={cn("text-sm font-medium truncate", active && "text-primary")}>{i + 1}. {s.shortTitle}</p>
                      <p className="text-[10px] text-muted-foreground truncate">{statusBadge[st].label}</p>
                    </div>
                  </button>
                );
              })}
            </div>
          </CardContent>
        </Card>

        {/* Stage form */}
        <Card className="shadow-card">
          <CardHeader>
            <div className="flex items-start justify-between gap-3">
              <div>
                <CardTitle className="text-base flex items-center gap-2">
                  <StageIcon className="h-4 w-4 text-primary" /> {meta.title}
                </CardTitle>
                <CardDescription>{meta.description}</CardDescription>
              </div>
              <Badge className={cn("border-0", statusBadge[status].className)}>{statusBadge[status].label}</Badge>
            </div>
            {status === "rejected" && store.rejectionNotes[meta.id] && (
              <div className="mt-3 rounded-lg bg-destructive/10 border border-destructive/20 p-3 flex gap-2">
                <AlertCircle className="h-4 w-4 text-destructive mt-0.5 shrink-0" />
                <div className="text-xs">
                  <p className="font-medium text-destructive">Reviewer note</p>
                  <p className="text-muted-foreground mt-0.5">{store.rejectionNotes[meta.id]}</p>
                </div>
              </div>
            )}
          </CardHeader>
          <CardContent>{renderStageBody()}</CardContent>
        </Card>

        {/* Timeline */}
        <Card className="shadow-card h-fit">
          <CardHeader className="pb-2"><CardTitle className="text-sm">Activity timeline</CardTitle></CardHeader>
          <CardContent>
            {store.timeline.length === 0 ? (
              <p className="text-xs text-muted-foreground">No activity yet. Start filling the form to see updates here.</p>
            ) : (
              <ol className="space-y-3 max-h-[420px] overflow-auto pr-1">
                {store.timeline.map((ev) => (
                  <li key={ev.id} className="flex gap-2 text-xs">
                    <span className={cn(
                      "mt-1 h-2 w-2 rounded-full shrink-0",
                      ev.status === "verified" ? "bg-success" :
                      ev.status === "rejected" ? "bg-destructive" :
                      ev.status === "submitted" ? "bg-primary" : "bg-muted-foreground"
                    )} />
                    <div className="min-w-0">
                      <p className="leading-snug">{ev.message}</p>
                      <p className="text-muted-foreground mt-0.5">{new Date(ev.at).toLocaleString("en-IN", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" })}</p>
                    </div>
                  </li>
                ))}
              </ol>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Nav */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Button variant="outline" onClick={goPrev} disabled={stageIdx === 0} className="gap-1.5">
          <ChevronLeft className="h-4 w-4" /> Previous
        </Button>
        <div className="flex flex-wrap items-center gap-2">
          {meta.id !== "golive" && status !== "submitted" && status !== "verified" && (
            <Button onClick={handleSubmitStage} className="gap-1.5">
              <ShieldCheck className="h-4 w-4" /> Submit for verification
            </Button>
          )}
          {status === "submitted" && (
            <Button variant="outline" onClick={handleSimulateVerify} className="gap-1.5">
              <CheckCircle2 className="h-4 w-4" /> Simulate verification
            </Button>
          )}
          {stageIdx < ONBOARDING_STAGES.length - 1 && (
            <Button variant="ghost" onClick={goNext} className="gap-1.5">
              Next <ChevronRight className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}