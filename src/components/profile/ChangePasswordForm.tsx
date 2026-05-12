import { useFormik } from "formik";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Loader2, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { PasswordInput } from "@/components/auth/PasswordInput";
import PasswordStrengthMeter from "@/components/auth/PasswordStrengthMeter";
import { changePasswordSchema } from "@/lib/profileValidation";
import { useProfile } from "@/hooks/useProfile";
import { formatDistanceToNowStrict } from "date-fns";

export function ChangePasswordForm() {
  const { profile, changePassword, savingScope } = useProfile();
  const isSaving = savingScope === "password";

  const formik = useFormik({
    initialValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
    validationSchema: changePasswordSchema,
    onSubmit: async (values, helpers) => {
      try {
        await changePassword(values.currentPassword, values.newPassword);
        toast.success("Password updated");
        helpers.resetForm();
      } catch (e) {
        toast.error(e instanceof Error ? e.message : "Could not update password");
      }
    },
  });

  return (
    <Card className="shadow-card">
      <CardHeader className="pb-3 flex flex-row items-center justify-between">
        <CardTitle className="text-base">Change Password</CardTitle>
        {profile?.passwordChangedAt && (
          <span className="text-[11px] text-muted-foreground flex items-center gap-1">
            <ShieldCheck className="h-3 w-3" />
            Last changed {formatDistanceToNowStrict(new Date(profile.passwordChangedAt))} ago
          </span>
        )}
      </CardHeader>
      <CardContent>
        <form onSubmit={formik.handleSubmit} className="space-y-4 max-w-md">
          <Field name="currentPassword" label="Current Password" formik={formik} />
          <div>
            <Field name="newPassword" label="New Password" formik={formik} />
            <div className="mt-2"><PasswordStrengthMeter password={formik.values.newPassword} /></div>
          </div>
          <Field name="confirmPassword" label="Confirm New Password" formik={formik} />
          <Button type="submit" disabled={!formik.isValid || isSaving || !formik.dirty} className="gap-1">
            {isSaving && <Loader2 className="h-4 w-4 animate-spin" />} Update password
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}

function Field({ name, label, formik }: { name: string; label: string; formik: ReturnType<typeof useFormik<Record<string, string>>>; }) {
  const err = formik.touched[name] && formik.errors[name];
  return (
    <div>
      <Label className="text-xs">{label}</Label>
      <PasswordInput
        value={formik.values[name] ?? ""}
        onChange={(e) => formik.setFieldValue(name, e.target.value)}
        onBlur={() => formik.setFieldTouched(name, true)}
        className="mt-1"
        autoComplete={name === "currentPassword" ? "current-password" : "new-password"}
      />
      {err && <p className="text-[11px] text-destructive mt-1">{String(err)}</p>}
    </div>
  );
}