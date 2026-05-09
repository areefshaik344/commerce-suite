import { useEffect, useState } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import OtpInput from "@/components/auth/OtpInput";
import { Mail, ArrowLeft, AlertCircle, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { useOtpTimer } from "@/hooks/useOtpTimer";
import { authApi } from "@/api/authApi";
import { ApiError } from "@/api/apiClient";
import { useAuth } from "@/hooks/useAuth";

export default function EmailVerificationPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const email = (location.state as { email?: string } | null)?.email || user?.email || "";

  const [otp, setOtp] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [verifying, setVerifying] = useState(false);
  const [resending, setResending] = useState(false);
  const timer = useOtpTimer({ autoStart: false });

  // Send the initial OTP once the page mounts.
  useEffect(() => {
    if (!email) return;
    void send(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [email]);

  const send = async (silent = false) => {
    if (!email) return;
    setResending(true);
    setError(null);
    try {
      const res = await authApi.sendOtp(email, "verify-email");
      timer.reset();
      setOtp("");
      if (!silent) toast.success("OTP resent", { description: `Dev code: ${res.data.devCode}` });
      else toast.message(`Dev code: ${res.data.devCode}`);
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Failed to send OTP");
    } finally {
      setResending(false);
    }
  };

  const handleVerify = async (code?: string) => {
    const submitted = code || otp;
    if (submitted.length !== 6) return;
    if (timer.expired) {
      setError("OTP has expired. Please request a new one.");
      return;
    }
    setVerifying(true);
    setError(null);
    try {
      await authApi.verifyOtp(email, "verify-email", submitted);
      toast.success("Email verified!", { description: "Your account is now active." });
      navigate("/", { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Verification failed");
      setOtp("");
    } finally {
      setVerifying(false);
    }
  };

  if (!email) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6">
        <Card className="max-w-md w-full">
          <CardContent className="p-6 text-center space-y-4">
            <p className="text-sm text-muted-foreground">No email to verify. Please sign up or sign in first.</p>
            <Button asChild className="w-full"><Link to="/signup">Sign up</Link></Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-6">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <div className="h-16 w-16 rounded-2xl gradient-primary flex items-center justify-center mx-auto mb-4">
            <Mail className="h-8 w-8 text-primary-foreground" />
          </div>
          <h2 className="text-2xl font-display font-bold">Verify your email</h2>
          <p className="text-sm text-muted-foreground mt-1">
            We've sent a 6-digit code to <span className="font-medium text-foreground">{email}</span>
          </p>
        </div>

        <Card className="shadow-elevated border-0">
          <CardContent className="p-6 space-y-5">
            {error && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <OtpInput value={otp} onChange={setOtp} onComplete={(v) => handleVerify(v)}
              invalid={!!error || timer.expired} disabled={verifying} />
            <Button className="w-full" onClick={() => handleVerify()} disabled={otp.length !== 6 || verifying || timer.expired}>
              {verifying ? <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Verifying...</> : "Verify Email"}
            </Button>
            <div className="flex items-center justify-between text-xs">
              <span className="text-muted-foreground">
                {timer.canResend ? "OTP expired" : `Code expires in ${timer.formatted}`}
              </span>
              <button
                onClick={() => send(false)}
                disabled={!timer.canResend || resending}
                className="text-primary hover:underline disabled:text-muted-foreground disabled:no-underline disabled:cursor-not-allowed"
              >
                {resending ? "Sending..." : "Resend OTP"}
              </button>
            </div>
          </CardContent>
        </Card>

        <div className="text-center">
          <Link to="/login" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-primary">
            <ArrowLeft className="h-3 w-3" /> Back to Login
          </Link>
        </div>
      </div>
    </div>
  );
}
