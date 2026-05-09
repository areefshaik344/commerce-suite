import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Formik, Form } from "formik";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { ArrowLeft, Mail, Check, Loader2, AlertCircle } from "lucide-react";
import OtpInput from "@/components/auth/OtpInput";
import { authApi } from "@/api/authApi";
import { ApiError } from "@/api/apiClient";
import { forgotPasswordSchema, otpSchema } from "@/lib/validation";
import { useOtpTimer } from "@/hooks/useOtpTimer";
import { toast } from "sonner";

export default function ForgotPasswordPage() {
  const [step, setStep] = useState<"request" | "verify">("request");
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [verifying, setVerifying] = useState(false);
  const [resending, setResending] = useState(false);
  const timer = useOtpTimer({ autoStart: false });
  const navigate = useNavigate();

  const handleResend = async () => {
    setResending(true);
    setError(null);
    try {
      const res = await authApi.forgotPassword(email);
      timer.reset();
      setOtp("");
      toast.success("Reset code resent", { description: res.data.devCode ? `Dev code: ${res.data.devCode}` : undefined });
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Failed to resend");
    } finally {
      setResending(false);
    }
  };

  const handleVerify = async (code?: string) => {
    const submitted = code || otp;
    try {
      await otpSchema.validate({ otp: submitted });
    } catch {
      setError("Enter the 6-digit code");
      return;
    }
    if (timer.expired) {
      setError("Code expired. Please request a new one.");
      return;
    }
    setVerifying(true);
    setError(null);
    try {
      // Verify only — actual reset happens on the next page using the same code.
      await authApi.verifyOtp(email, "reset-password", submitted);
      // Re-issue immediately so the reset endpoint can verify again.
      await authApi.sendOtp(email, "reset-password");
      navigate("/reset-password", { state: { email, otp: submitted } });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Verification failed");
      setOtp("");
    } finally {
      setVerifying(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-6">
      <div className="w-full max-w-md space-y-6">
        <Link to="/login" className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors">
          <ArrowLeft className="h-4 w-4" /> Back to login
        </Link>

        <Card className="shadow-elevated border-0">
          <CardHeader className="text-center">
            <div className="mx-auto h-12 w-12 rounded-full gradient-primary flex items-center justify-center mb-2">
              {step === "verify" ? <Check className="h-6 w-6 text-primary-foreground" /> : <Mail className="h-6 w-6 text-primary-foreground" />}
            </div>
            <CardTitle className="text-xl font-display">
              {step === "request" ? "Reset password" : "Enter reset code"}
            </CardTitle>
            <CardDescription>
              {step === "request"
                ? "Enter your email and we'll send a one-time code."
                : `We sent a 6-digit code to ${email}`}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {step === "request" ? (
              <Formik
                initialValues={{ email: "" }}
                validationSchema={forgotPasswordSchema}
                onSubmit={async (values, { setSubmitting }) => {
                  try {
                    const res = await authApi.forgotPassword(values.email.trim());
                    setEmail(values.email.trim());
                    setStep("verify");
                    timer.reset();
                    toast.success("Code sent", { description: res.data.devCode ? `Dev code: ${res.data.devCode}` : "Check your inbox" });
                  } catch (err) {
                    toast.error(err instanceof ApiError ? err.message : "Could not send code");
                  } finally {
                    setSubmitting(false);
                  }
                }}
              >
                {({ values, errors, touched, handleChange, handleBlur, isSubmitting }) => (
                  <Form className="space-y-4" noValidate>
                    <div className="space-y-2">
                      <Label htmlFor="email">Email address</Label>
                      <Input id="email" name="email" type="email" autoComplete="email"
                        placeholder="rahul@example.com" value={values.email}
                        onChange={handleChange} onBlur={handleBlur}
                        aria-invalid={!!(touched.email && errors.email)} />
                      {touched.email && errors.email && <p className="text-xs text-destructive">{errors.email}</p>}
                    </div>
                    <Button type="submit" className="w-full" disabled={isSubmitting}>
                      {isSubmitting ? <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Sending...</> : "Send Reset Code"}
                    </Button>
                  </Form>
                )}
              </Formik>
            ) : (
              <div className="space-y-4">
                {error && (
                  <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription>{error}</AlertDescription>
                  </Alert>
                )}
                <OtpInput value={otp} onChange={setOtp} onComplete={(v) => handleVerify(v)}
                  invalid={!!error || timer.expired} disabled={verifying} />
                <Button className="w-full" disabled={otp.length !== 6 || verifying || timer.expired}
                  onClick={() => handleVerify()}>
                  {verifying ? <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Verifying...</> : "Verify code"}
                </Button>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">
                    {timer.canResend ? "Code expired" : `Expires in ${timer.formatted}`}
                  </span>
                  <button onClick={handleResend} disabled={!timer.canResend || resending}
                    className="text-primary hover:underline disabled:text-muted-foreground disabled:no-underline disabled:cursor-not-allowed">
                    {resending ? "Sending..." : "Resend code"}
                  </button>
                </div>
                <button type="button" onClick={() => { setStep("request"); setOtp(""); setError(null); }}
                  className="text-xs text-muted-foreground hover:text-foreground w-full">
                  Use a different email
                </button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
