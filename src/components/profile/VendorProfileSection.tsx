import { useFormik } from "formik";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { BadgeCheck, ShieldAlert, Loader2, Building2 } from "lucide-react";
import { toast } from "sonner";
import { useProfile } from "@/hooks/useProfile";
import { vendorBusinessSchema } from "@/lib/profileValidation";
import { EmptyState } from "@/components/shared/EmptyState";
import { useNavigate } from "react-router-dom";

const VERIFICATION: Record<string, { label: string; className: string; icon: typeof BadgeCheck }> = {
  verified: { label: "Verified seller", className: "bg-success/10 text-success border-0", icon: BadgeCheck },
  pending:  { label: "Verification pending", className: "bg-warning/10 text-warning border-0", icon: ShieldAlert },
  rejected: { label: "Verification rejected", className: "bg-destructive/10 text-destructive border-0", icon: ShieldAlert },
  unverified: { label: "Not verified", className: "bg-muted text-foreground border-0", icon: ShieldAlert },
};

export function VendorProfileSection() {
  const { vendorProfile, updateVendorProfile, savingScope, profile } = useProfile();
  const navigate = useNavigate();
  const isSaving = savingScope === "vendor";

  if (!vendorProfile) {
    return (
      <EmptyState
        icon={Building2}
        title="No vendor profile yet"
        description={profile?.vendorStatus === "pending"
          ? "Your seller application is under review. You can complete your business profile once approved."
          : "Become a seller to set up your business profile, GSTIN, store slug and bank details."}
        actionLabel={profile?.vendorStatus === "pending" ? "Track application" : "Apply as seller"}
        onAction={() => navigate("/vendor/onboarding")}
      />
    );
  }

  const v = VERIFICATION[vendorProfile.verificationStatus] ?? VERIFICATION.unverified;
  const Icon = v.icon;

  const formik = useFormik({
    enableReinitialize: true,
    initialValues: {
      businessName: vendorProfile.businessName,
      legalName: vendorProfile.legalName,
      storeSlug: vendorProfile.storeSlug,
      supportEmail: vendorProfile.supportEmail,
      supportPhone: vendorProfile.supportPhone,
      description: vendorProfile.description,
      category: vendorProfile.category,
      gstin: vendorProfile.gstin,
      pan: vendorProfile.pan,
    },
    validationSchema: vendorBusinessSchema,
    onSubmit: async (values, helpers) => {
      try {
        await updateVendorProfile(values);
        toast.success("Vendor profile updated");
        helpers.resetForm({ values });
      } catch (e) {
        toast.error(e instanceof Error ? e.message : "Update failed");
      }
    },
  });

  return (
    <div className="space-y-4">
      <Card className="shadow-card">
        <CardHeader className="pb-3 flex flex-row items-center justify-between">
          <CardTitle className="text-base flex items-center gap-2">
            <Building2 className="h-4 w-4" />Business Profile
          </CardTitle>
          <Badge className={`text-[10px] gap-1 ${v.className}`}><Icon className="h-3 w-3" />{v.label}</Badge>
        </CardHeader>
        <CardContent>
          <form onSubmit={formik.handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <F name="businessName" label="Business name" formik={formik} />
              <F name="legalName" label="Legal entity name" formik={formik} />
              <F name="storeSlug" label="Store slug (storefront URL)" formik={formik} prefix={`${window.location.origin}/store/`} />
              <F name="category" label="Primary category" formik={formik} />
              <F name="supportEmail" label="Support email" formik={formik} />
              <F name="supportPhone" label="Support phone" formik={formik} />
              <F name="gstin" label="GSTIN" formik={formik} />
              <F name="pan" label="PAN" formik={formik} />
            </div>
            <div>
              <Label className="text-xs">Store description</Label>
              <Textarea
                name="description" rows={3} maxLength={500}
                value={formik.values.description}
                onChange={formik.handleChange} onBlur={formik.handleBlur}
                className="mt-1"
              />
            </div>
            <Button type="submit" disabled={!formik.dirty || !formik.isValid || isSaving} className="gap-1">
              {isSaving && <Loader2 className="h-4 w-4 animate-spin" />} Save business profile
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card className="shadow-card">
        <CardHeader className="pb-3"><CardTitle className="text-base">Business Address</CardTitle></CardHeader>
        <CardContent>
          <p className="text-sm">{vendorProfile.businessAddress.line1}{vendorProfile.businessAddress.line2 ? `, ${vendorProfile.businessAddress.line2}` : ""}</p>
          <p className="text-sm text-muted-foreground">{vendorProfile.businessAddress.city}, {vendorProfile.businessAddress.state} - {vendorProfile.businessAddress.pincode}</p>
          <p className="text-xs text-muted-foreground mt-1">📞 {vendorProfile.businessAddress.phone}</p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => navigate("/vendor/settings")}>
            Manage in vendor settings
          </Button>
        </CardContent>
      </Card>

      <Card className="shadow-card">
        <CardHeader className="pb-3"><CardTitle className="text-base">Settlement Bank</CardTitle></CardHeader>
        <CardContent className="space-y-1 text-sm">
          <p><span className="text-muted-foreground">Account holder:</span> {vendorProfile.bank.accountHolder}</p>
          <p><span className="text-muted-foreground">Account:</span> {vendorProfile.bank.accountNumberMasked}</p>
          <p><span className="text-muted-foreground">Bank:</span> {vendorProfile.bank.bankName} · {vendorProfile.bank.branch}</p>
          <p><span className="text-muted-foreground">IFSC:</span> {vendorProfile.bank.ifsc}</p>
          <Badge className={vendorProfile.bank.verified ? "bg-success/10 text-success border-0" : "bg-warning/10 text-warning border-0"}>
            {vendorProfile.bank.verified ? "Bank verified" : "Awaiting verification"}
          </Badge>
        </CardContent>
      </Card>
    </div>
  );
}

function F({ name, label, formik, prefix }: { name: string; label: string; formik: ReturnType<typeof useFormik<Record<string, string>>>; prefix?: string; }) {
  const err = formik.touched[name] && formik.errors[name];
  return (
    <div>
      <Label className="text-xs">{label}</Label>
      <div className="flex items-center gap-2 mt-1">
        {prefix && <span className="text-[11px] text-muted-foreground truncate max-w-[180px]">{prefix}</span>}
        <Input
          name={name} value={formik.values[name] ?? ""}
          onChange={formik.handleChange} onBlur={formik.handleBlur}
        />
      </div>
      {err && <p className="text-[11px] text-destructive mt-1">{String(err)}</p>}
    </div>
  );
}