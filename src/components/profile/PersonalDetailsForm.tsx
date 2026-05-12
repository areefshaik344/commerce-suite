import { useEffect, useState } from "react";
import { useFormik } from "formik";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Loader2, ShieldCheck, ShieldAlert } from "lucide-react";
import { toast } from "sonner";
import { useProfile } from "@/hooks/useProfile";
import { personalDetailsSchema } from "@/lib/profileValidation";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { AvatarUploader } from "./AvatarUploader";

export function PersonalDetailsForm() {
  const { profile, updatePersonal, savingScope } = useProfile();
  const [confirmDiscard, setConfirmDiscard] = useState(false);

  const formik = useFormik({
    enableReinitialize: true,
    initialValues: {
      name: profile?.name ?? "",
      email: profile?.email ?? "",
      phone: (profile?.phone ?? "").replace(/^\+91\s?/, ""),
      gender: (profile?.gender ?? "") as string,
      dob: profile?.dob ?? "",
      bio: profile?.bio ?? "",
    },
    validationSchema: personalDetailsSchema,
    onSubmit: async (values, helpers) => {
      try {
        await updatePersonal({
          name: values.name,
          phone: `+91 ${values.phone}`,
          // email is read-only here (would need OTP verification flow to change)
          gender: values.gender as never,
          dob: values.dob || undefined,
          bio: values.bio,
        });
        toast.success("Profile saved");
        helpers.resetForm({ values });
      } catch (err) {
        toast.error(err instanceof Error ? err.message : "Save failed");
      }
    },
  });

  // Warn on tab close with unsaved changes
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (formik.dirty && !formik.isSubmitting) { e.preventDefault(); e.returnValue = ""; }
    };
    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [formik.dirty, formik.isSubmitting]);

  if (!profile) return null;
  const isSaving = savingScope === "personal";

  return (
    <Card className="shadow-card">
      <CardHeader className="pb-3">
        <CardTitle className="text-base">Personal Information</CardTitle>
      </CardHeader>
      <CardContent className="space-y-5">
        <AvatarUploader />

        <form onSubmit={formik.handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="Full Name" name="name" formik={formik} />
            <div>
              <div className="flex items-center justify-between">
                <Label className="text-xs">Email</Label>
                {profile.emailVerified
                  ? <Badge className="text-[10px] bg-success/10 text-success border-0 gap-1"><ShieldCheck className="h-3 w-3" />Verified</Badge>
                  : <Badge variant="outline" className="text-[10px] gap-1"><ShieldAlert className="h-3 w-3" />Unverified</Badge>}
              </div>
              <Input value={formik.values.email} disabled className="mt-1" />
              <p className="text-[10px] text-muted-foreground mt-1">Contact support to change your email.</p>
            </div>
            <div>
              <div className="flex items-center justify-between">
                <Label className="text-xs">Phone</Label>
                {profile.phoneVerified
                  ? <Badge className="text-[10px] bg-success/10 text-success border-0 gap-1"><ShieldCheck className="h-3 w-3" />Verified</Badge>
                  : <Badge variant="outline" className="text-[10px] gap-1"><ShieldAlert className="h-3 w-3" />Unverified</Badge>}
              </div>
              <div className="flex items-center gap-2 mt-1">
                <span className="text-sm text-muted-foreground">+91</span>
                <Input
                  value={formik.values.phone}
                  onChange={(e) => formik.setFieldValue("phone", e.target.value.replace(/\D/g, "").slice(0, 10))}
                  onBlur={formik.handleBlur} name="phone" inputMode="numeric"
                />
              </div>
              {formik.touched.phone && formik.errors.phone && (
                <p className="text-[11px] text-destructive mt-1">{formik.errors.phone}</p>
              )}
            </div>
            <div>
              <Label className="text-xs">Gender</Label>
              <Select value={formik.values.gender} onValueChange={(v) => formik.setFieldValue("gender", v)}>
                <SelectTrigger className="mt-1"><SelectValue placeholder="Select" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="male">Male</SelectItem>
                  <SelectItem value="female">Female</SelectItem>
                  <SelectItem value="other">Other</SelectItem>
                  <SelectItem value="prefer_not_to_say">Prefer not to say</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <Field label="Date of Birth" name="dob" type="date" formik={formik} />
          </div>
          <div>
            <Label className="text-xs">About</Label>
            <Textarea
              name="bio" value={formik.values.bio} onChange={formik.handleChange} onBlur={formik.handleBlur}
              rows={3} maxLength={280} className="mt-1"
              placeholder="Tell others a little about yourself…"
            />
            <div className="flex items-center justify-between mt-1">
              {formik.touched.bio && formik.errors.bio
                ? <p className="text-[11px] text-destructive">{formik.errors.bio}</p>
                : <span />}
              <span className="text-[10px] text-muted-foreground">{formik.values.bio.length}/280</span>
            </div>
          </div>

          <div className="flex items-center gap-2 pt-2">
            <Button type="submit" disabled={!formik.dirty || !formik.isValid || isSaving} className="gap-1">
              {isSaving && <Loader2 className="h-4 w-4 animate-spin" />} Save changes
            </Button>
            <Button
              type="button" variant="ghost" disabled={!formik.dirty || isSaving}
              onClick={() => setConfirmDiscard(true)}
            >
              Discard
            </Button>
            {formik.dirty && <span className="text-[11px] text-muted-foreground">Unsaved changes</span>}
          </div>
        </form>
      </CardContent>
      <ConfirmDialog
        open={confirmDiscard} onOpenChange={setConfirmDiscard}
        title="Discard changes?" description="You have unsaved changes. Discard them?"
        confirmLabel="Discard" variant="destructive"
        onConfirm={() => formik.resetForm()}
      />
    </Card>
  );
}

function Field({ label, name, formik, type = "text" }: { label: string; name: string; formik: ReturnType<typeof useFormik<Record<string, string>>>; type?: string; }) {
  const error = formik.touched[name] && formik.errors[name];
  return (
    <div>
      <Label className="text-xs">{label}</Label>
      <Input
        name={name} type={type} value={formik.values[name] ?? ""}
        onChange={formik.handleChange} onBlur={formik.handleBlur}
        className="mt-1"
      />
      {error && <p className="text-[11px] text-destructive mt-1">{String(error)}</p>}
    </div>
  );
}