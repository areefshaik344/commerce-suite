import { useState } from "react";
import { Link, useLocation, useNavigate, Navigate } from "react-router-dom";
import { Formik, Form } from "formik";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { ArrowLeft, KeyRound, Check, Loader2, AlertCircle } from "lucide-react";
import { toast } from "sonner";
import PasswordInput from "@/components/auth/PasswordInput";
import PasswordStrengthMeter from "@/components/auth/PasswordStrengthMeter";
import { resetPasswordSchema } from "@/lib/validation";
import { authApi } from "@/api/authApi";
import { ApiError } from "@/api/apiClient";

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as { email?: string; otp?: string } | null;
  const [done, setDone] = useState(false);
  const [globalError, setGlobalError] = useState<string | null>(null);

  // Hard guard: this page must only be reached via /forgot-password.
  if (!state?.email || !state?.otp) {
    return <Navigate to="/forgot-password" replace />;
  }

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-6">
      <div className="w-full max-w-md space-y-6">
        <Link to="/login" className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors">
          <ArrowLeft className="h-4 w-4" /> Back to login
        </Link>

        <Card className="shadow-elevated border-0">
          <CardHeader className="text-center">
            <div className="mx-auto h-12 w-12 rounded-full gradient-primary flex items-center justify-center mb-2">
              {done ? <Check className="h-6 w-6 text-primary-foreground" /> : <KeyRound className="h-6 w-6 text-primary-foreground" />}
            </div>
            <CardTitle className="text-xl font-display">{done ? "Password Updated" : "Set New Password"}</CardTitle>
            <CardDescription>
              {done ? "Your password has been reset successfully." : "Choose a strong, unique password."}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {done ? (
              <Button asChild className="w-full">
                <Link to="/login">Go to Login</Link>
              </Button>
            ) : (
              <>
                {globalError && (
                  <Alert variant="destructive" className="mb-3">
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription>{globalError}</AlertDescription>
                  </Alert>
                )}
                <Formik
                  initialValues={{ password: "", confirmPassword: "" }}
                  validationSchema={resetPasswordSchema}
                  onSubmit={async (values, { setSubmitting }) => {
                    setGlobalError(null);
                    try {
                      await authApi.resetPassword(state.email!, state.otp!, values.password);
                      setDone(true);
                      toast.success("Password reset successful");
                      setTimeout(() => navigate("/login", { replace: true }), 1500);
                    } catch (err) {
                      const message = err instanceof ApiError ? err.message : "Could not reset password";
                      setGlobalError(message);
                      if (err instanceof ApiError && err.status === 410) {
                        toast.error("Reset link expired. Start over.");
                        setTimeout(() => navigate("/forgot-password", { replace: true }), 1500);
                      }
                    } finally {
                      setSubmitting(false);
                    }
                  }}
                >
                  {({ values, errors, touched, handleChange, handleBlur, isSubmitting }) => (
                    <Form className="space-y-4" noValidate>
                      <div className="space-y-2">
                        <Label htmlFor="password">New Password</Label>
                        <PasswordInput id="password" name="password" autoComplete="new-password"
                          placeholder="••••••••" value={values.password}
                          onChange={handleChange} onBlur={handleBlur}
                          invalid={!!(touched.password && errors.password)} />
                        <PasswordStrengthMeter password={values.password} />
                        {touched.password && errors.password && (
                          <p className="text-xs text-destructive">{errors.password}</p>
                        )}
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="confirmPassword">Confirm New Password</Label>
                        <PasswordInput id="confirmPassword" name="confirmPassword" autoComplete="new-password"
                          placeholder="••••••••" value={values.confirmPassword}
                          onChange={handleChange} onBlur={handleBlur}
                          invalid={!!(touched.confirmPassword && errors.confirmPassword)} />
                        {touched.confirmPassword && errors.confirmPassword && (
                          <p className="text-xs text-destructive">{errors.confirmPassword}</p>
                        )}
                      </div>
                      <Button type="submit" className="w-full" disabled={isSubmitting}>
                        {isSubmitting ? <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Resetting...</> : "Reset Password"}
                      </Button>
                    </Form>
                  )}
                </Formik>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
