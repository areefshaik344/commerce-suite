import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Formik, Form } from "formik";
import { toFormikValidationSchema as _ } from "yup"; // type-only marker; not used directly
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Checkbox } from "@/components/ui/checkbox";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Mail, Phone, ShieldCheck, ChevronRight, Truck, Tag, Headphones, AlertCircle, Loader2 } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { toast } from "sonner";
import OtpInput from "@/components/auth/OtpInput";
import PasswordInput from "@/components/auth/PasswordInput";
import { loginSchema } from "@/lib/validation";
import { ApiError } from "@/api/apiClient";
import { authApi } from "@/api/authApi";
import { useOtpTimer } from "@/hooks/useOtpTimer";

// Suppress unused-import warning for the type marker
void _;

export default function LoginPage() {
  const [tab, setTab] = useState<"email" | "phone">("email");
  const [phone, setPhone] = useState("");
  const [otp, setOtp] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [otpError, setOtpError] = useState<string | null>(null);
  const [phoneLoading, setPhoneLoading] = useState(false);
  const [globalError, setGlobalError] = useState<string | null>(null);

  const { loginAsync } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from || "/";
  const otpTimer = useOtpTimer({ autoStart: false });

  const handleSendOtp = async () => {
    if (!/^\d{10}$/.test(phone)) {
      toast.error("Enter a valid 10-digit phone number");
      return;
    }
    setPhoneLoading(true);
    setOtpError(null);
    try {
      const res = await authApi.sendOtp(`+91${phone}`, "phone-login");
      setOtpSent(true);
      setOtp("");
      otpTimer.reset();
      toast.success(`OTP sent to +91 ${phone}`, { description: `Dev code: ${res.data.devCode}` });
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Failed to send OTP");
    } finally {
      setPhoneLoading(false);
    }
  };

  const handleVerifyOtp = async () => {
    if (otp.length !== 6) return;
    if (otpTimer.expired) {
      setOtpError("OTP has expired. Please request a new one.");
      return;
    }
    setPhoneLoading(true);
    setOtpError(null);
    try {
      await authApi.verifyOtp(`+91${phone}`, "phone-login", otp);
      // Demo: log the user in as the first customer with this phone (mock backend cannot create new accounts here).
      // Real backend would issue tokens for the matched phone account.
      await loginAsync("rahul@example.com", "password");
      toast.success("Welcome back!");
      navigate(from, { replace: true });
    } catch (err) {
      const message = err instanceof ApiError ? err.message : "Verification failed";
      setOtpError(message);
    } finally {
      setPhoneLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex">
      {/* Left panel - branding */}
      <div className="hidden lg:flex lg:w-[480px] gradient-primary flex-col justify-between p-10 text-primary-foreground relative overflow-hidden">
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-20 -left-10 w-60 h-60 rounded-full bg-white/20 blur-3xl" />
          <div className="absolute bottom-20 right-0 w-80 h-80 rounded-full bg-white/10 blur-3xl" />
        </div>
        <div className="relative z-10">
          <Link to="/"><h1 className="text-3xl font-display font-bold">MarketHub</h1></Link>
          <p className="text-sm mt-1 opacity-80">India's favourite multi-vendor marketplace</p>
        </div>
        <div className="relative z-10 space-y-6">
          <FeatureRow icon={Truck} title="Free Delivery" subtitle="On orders above ₹499" />
          <FeatureRow icon={Tag} title="Best Prices" subtitle="Verified by millions of shoppers" />
          <FeatureRow icon={Headphones} title="24/7 Support" subtitle="Dedicated customer support" />
        </div>
        <p className="relative z-10 text-xs opacity-60">© 2025 MarketHub. All rights reserved.</p>
      </div>

      {/* Right panel - form */}
      <div className="flex-1 flex items-center justify-center p-6">
        <div className="w-full max-w-md space-y-6">
          <div className="text-center lg:text-left">
            <h2 className="text-2xl font-display font-bold">Welcome back</h2>
            <p className="text-sm text-muted-foreground mt-1">Log in to access your account</p>
          </div>

          <Card className="shadow-elevated border-0">
            <CardContent className="p-6">
              <Tabs value={tab} onValueChange={(v) => { setTab(v as "email" | "phone"); setOtpSent(false); setGlobalError(null); }}>
                <TabsList className="grid grid-cols-2 w-full">
                  <TabsTrigger value="email" className="gap-1.5 text-xs">
                    <Mail className="h-3.5 w-3.5" /> Email
                  </TabsTrigger>
                  <TabsTrigger value="phone" className="gap-1.5 text-xs">
                    <Phone className="h-3.5 w-3.5" /> Phone OTP
                  </TabsTrigger>
                </TabsList>

                <TabsContent value="email" className="mt-4">
                  {globalError && (
                    <Alert variant="destructive" className="mb-3">
                      <AlertCircle className="h-4 w-4" />
                      <AlertDescription>{globalError}</AlertDescription>
                    </Alert>
                  )}
                  <Formik
                    initialValues={{ email: "", password: "", rememberMe: true }}
                    validationSchema={loginSchema}
                    onSubmit={async (values, { setSubmitting }) => {
                      setGlobalError(null);
                      try {
                        await loginAsync(values.email.trim(), values.password);
                        toast.success("Welcome back!");
                        navigate(from, { replace: true });
                      } catch (err) {
                        const message = err instanceof ApiError ? err.message : "Something went wrong";
                        setGlobalError(message);
                      } finally {
                        setSubmitting(false);
                      }
                    }}
                  >
                    {({ values, errors, touched, handleChange, handleBlur, setFieldValue, isSubmitting }) => (
                      <Form className="space-y-4" noValidate>
                        <div className="space-y-2">
                          <Label htmlFor="email">Email address</Label>
                          <Input
                            id="email" name="email" type="email" autoComplete="email"
                            placeholder="rahul@example.com"
                            value={values.email} onChange={handleChange} onBlur={handleBlur}
                            aria-invalid={!!(touched.email && errors.email)}
                          />
                          {touched.email && errors.email && (
                            <p className="text-xs text-destructive">{errors.email}</p>
                          )}
                        </div>
                        <div className="space-y-2">
                          <div className="flex items-center justify-between">
                            <Label htmlFor="password">Password</Label>
                            <Link to="/forgot-password" className="text-xs text-primary hover:underline">
                              Forgot password?
                            </Link>
                          </div>
                          <PasswordInput
                            id="password" name="password" autoComplete="current-password"
                            placeholder="Enter password"
                            value={values.password} onChange={handleChange} onBlur={handleBlur}
                            invalid={!!(touched.password && errors.password)}
                          />
                          {touched.password && errors.password && (
                            <p className="text-xs text-destructive">{errors.password}</p>
                          )}
                        </div>
                        <div className="flex items-center gap-2">
                          <Checkbox id="rememberMe" checked={values.rememberMe}
                            onCheckedChange={(v) => setFieldValue("rememberMe", v === true)} />
                          <label htmlFor="rememberMe" className="text-xs text-muted-foreground">Remember me on this device</label>
                        </div>
                        <Button type="submit" className="w-full" disabled={isSubmitting}>
                          {isSubmitting ? <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Signing in...</> : "Sign In"}
                        </Button>
                      </Form>
                    )}
                  </Formik>
                  <p className="text-xs text-muted-foreground mt-3 text-center">
                    Demo: <span className="font-mono bg-muted px-1 rounded">rahul@example.com</span> / <span className="font-mono bg-muted px-1 rounded">password</span>
                  </p>
                </TabsContent>

                <TabsContent value="phone" className="mt-4">
                  {!otpSent ? (
                    <div className="space-y-4">
                      <div className="space-y-2">
                        <Label>Mobile Number</Label>
                        <div className="flex gap-2">
                          <div className="flex items-center px-3 rounded-md border bg-muted text-sm font-medium text-muted-foreground">+91</div>
                          <Input
                            placeholder="98765 43210"
                            value={phone}
                            onChange={(e) => setPhone(e.target.value.replace(/\D/g, "").slice(0, 10))}
                            maxLength={10}
                            inputMode="numeric"
                          />
                        </div>
                      </div>
                      <Button onClick={handleSendOtp} className="w-full" disabled={phoneLoading || phone.length !== 10}>
                        {phoneLoading ? <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Sending OTP...</> : "Send OTP"}
                      </Button>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      <div className="text-center">
                        <p className="text-sm text-muted-foreground">
                          OTP sent to <span className="font-medium text-foreground">+91 {phone}</span>
                        </p>
                        <button onClick={() => setOtpSent(false)} className="text-xs text-primary hover:underline mt-1">Change number</button>
                      </div>
                      <OtpInput value={otp} onChange={setOtp} invalid={!!otpError || otpTimer.expired} onComplete={handleVerifyOtp} />
                      {(otpError || otpTimer.expired) && (
                        <p className="text-xs text-destructive text-center">{otpError || "OTP expired — request a new code"}</p>
                      )}
                      <Button onClick={handleVerifyOtp} className="w-full" disabled={phoneLoading || otp.length !== 6 || otpTimer.expired}>
                        {phoneLoading ? <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Verifying...</> : "Verify & Login"}
                      </Button>
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-muted-foreground">{otpTimer.canResend ? "OTP expired" : `Expires in ${otpTimer.formatted}`}</span>
                        <button
                          onClick={handleSendOtp}
                          disabled={!otpTimer.canResend || phoneLoading}
                          className="text-primary hover:underline disabled:text-muted-foreground disabled:no-underline disabled:cursor-not-allowed"
                        >
                          Resend OTP
                        </button>
                      </div>
                    </div>
                  )}
                </TabsContent>
              </Tabs>
            </CardContent>
          </Card>

          <div className="text-center space-y-3">
            <p className="text-sm text-muted-foreground">
              New to MarketHub?{" "}
              <Link to="/signup" className="text-primary font-medium hover:underline">Create an account</Link>
            </p>
            <Separator />
            <Link to="/vendor/register" className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-primary transition-colors">
              <ShieldCheck className="h-4 w-4" /> Sell on MarketHub <ChevronRight className="h-3 w-3" />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

function FeatureRow({ icon: Icon, title, subtitle }: { icon: React.ElementType; title: string; subtitle: string }) {
  return (
    <div className="flex items-start gap-3">
      <div className="h-10 w-10 rounded-xl bg-white/15 flex items-center justify-center shrink-0">
        <Icon className="h-5 w-5" />
      </div>
      <div>
        <p className="font-semibold text-sm">{title}</p>
        <p className="text-xs opacity-70">{subtitle}</p>
      </div>
    </div>
  );
}
    e.preventDefault();
    setLoading(true);
    // Mock: simulate API delay
    await new Promise(r => setTimeout(r, 800));
    const success = loginWithCredentials(email, password);
    setLoading(false);
    if (success) {
      toast({ title: "Welcome back!", description: "You've been logged in successfully." });
      navigate(from, { replace: true });
    } else {
      toast({ title: "Invalid credentials", description: "Please check your email and password.", variant: "destructive" });
    }
  };

  const handleSendOTP = async () => {
    if (!phone || phone.length < 10) {
      toast({ title: "Invalid phone", description: "Enter a valid 10-digit phone number.", variant: "destructive" });
      return;
    }
    setLoading(true);
    await new Promise(r => setTimeout(r, 1000));
    setOtpSent(true);
    setLoading(false);
    toast({ title: "OTP Sent!", description: `A 6-digit OTP has been sent to +91 ${phone}` });
  };

  const handleVerifyOTP = async () => {
    setLoading(true);
    await new Promise(r => setTimeout(r, 800));
    // Mock: any 6-digit OTP works
    if (otp.length === 6) {
      loginWithCredentials(`${phone}@phone.mock`, "phone-otp");
      setLoading(false);
      toast({ title: "Welcome!", description: "Phone verified successfully." });
      navigate(from, { replace: true });
    } else {
      setLoading(false);
      toast({ title: "Invalid OTP", description: "Please enter a valid 6-digit OTP.", variant: "destructive" });
    }
  };

  return (
    <div className="min-h-screen bg-background flex">
      {/* Left panel - branding */}
      <div className="hidden lg:flex lg:w-[480px] gradient-primary flex-col justify-between p-10 text-primary-foreground relative overflow-hidden">
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-20 -left-10 w-60 h-60 rounded-full bg-white/20 blur-3xl" />
          <div className="absolute bottom-20 right-0 w-80 h-80 rounded-full bg-white/10 blur-3xl" />
        </div>
        <div className="relative z-10">
          <Link to="/">
            <h1 className="text-3xl font-display font-bold">MarketHub</h1>
          </Link>
          <p className="text-sm mt-1 opacity-80">India's favourite multi-vendor marketplace</p>
        </div>
        <div className="relative z-10 space-y-6">
          <div className="flex items-start gap-3">
            <div className="h-10 w-10 rounded-xl bg-white/15 flex items-center justify-center shrink-0">
              <Truck className="h-5 w-5" />
            </div>
            <div>
              <p className="font-semibold text-sm">Free Delivery</p>
              <p className="text-xs opacity-70">On orders above ₹499</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <div className="h-10 w-10 rounded-xl bg-white/15 flex items-center justify-center shrink-0">
              <Tag className="h-5 w-5" />
            </div>
            <div>
              <p className="font-semibold text-sm">Best Prices</p>
              <p className="text-xs opacity-70">Verified by millions of shoppers</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <div className="h-10 w-10 rounded-xl bg-white/15 flex items-center justify-center shrink-0">
              <Headphones className="h-5 w-5" />
            </div>
            <div>
              <p className="font-semibold text-sm">24/7 Support</p>
              <p className="text-xs opacity-70">Dedicated customer support</p>
            </div>
          </div>
        </div>
        <p className="relative z-10 text-xs opacity-60">© 2025 MarketHub. All rights reserved.</p>
      </div>

      {/* Right panel - form */}
      <div className="flex-1 flex items-center justify-center p-6">
        <div className="w-full max-w-md space-y-6">
          <div className="text-center lg:text-left">
            <h2 className="text-2xl font-display font-bold">Welcome back</h2>
            <p className="text-sm text-muted-foreground mt-1">Log in to access your account</p>
          </div>

          <Card className="shadow-elevated border-0">
            <CardContent className="p-6">
              <Tabs value={tab} onValueChange={(v) => { setTab(v as "email" | "phone"); setOtpSent(false); }}>
                <TabsList className="grid grid-cols-2 w-full">
                  <TabsTrigger value="email" className="gap-1.5 text-xs">
                    <Mail className="h-3.5 w-3.5" /> Email
                  </TabsTrigger>
                  <TabsTrigger value="phone" className="gap-1.5 text-xs">
                    <Phone className="h-3.5 w-3.5" /> Phone OTP
                  </TabsTrigger>
                </TabsList>

                <TabsContent value="email" className="mt-4">
                  <form onSubmit={handleEmailLogin} className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="email">Email address</Label>
                      <Input
                        id="email"
                        type="email"
                        placeholder="rahul@example.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                      />
                    </div>
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <Label htmlFor="password">Password</Label>
                        <Link to="/forgot-password" className="text-xs text-primary hover:underline">
                          Forgot password?
                        </Link>
                      </div>
                      <div className="relative">
                        <Input
                          id="password"
                          type={showPassword ? "text" : "password"}
                          placeholder="Enter password"
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          required
                        />
                        <button
                          type="button"
                          onClick={() => setShowPassword(!showPassword)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                        >
                          {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </button>
                      </div>
                    </div>
                    <Button type="submit" className="w-full" disabled={loading}>
                      {loading ? "Signing in..." : "Sign In"}
                    </Button>
                  </form>
                  <p className="text-xs text-muted-foreground mt-3 text-center">
                    Demo: use <span className="font-mono bg-muted px-1 rounded">rahul@example.com</span> / <span className="font-mono bg-muted px-1 rounded">password</span>
                  </p>
                </TabsContent>

                <TabsContent value="phone" className="mt-4">
                  {!otpSent ? (
                    <div className="space-y-4">
                      <div className="space-y-2">
                        <Label>Mobile Number</Label>
                        <div className="flex gap-2">
                          <div className="flex items-center px-3 rounded-md border bg-muted text-sm font-medium text-muted-foreground">
                            +91
                          </div>
                          <Input
                            placeholder="98765 43210"
                            value={phone}
                            onChange={(e) => setPhone(e.target.value.replace(/\D/g, "").slice(0, 10))}
                            maxLength={10}
                          />
                        </div>
                      </div>
                      <Button onClick={handleSendOTP} className="w-full" disabled={loading}>
                        {loading ? "Sending OTP..." : "Send OTP"}
                      </Button>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      <div className="text-center">
                        <p className="text-sm text-muted-foreground">
                          OTP sent to <span className="font-medium text-foreground">+91 {phone}</span>
                        </p>
                        <button onClick={() => setOtpSent(false)} className="text-xs text-primary hover:underline mt-1">
                          Change number
                        </button>
                      </div>
                      <OTPInput value={otp} onChange={setOtp} />
                      <Button onClick={handleVerifyOTP} className="w-full" disabled={loading || otp.length !== 6}>
                        {loading ? "Verifying..." : "Verify & Login"}
                      </Button>
                      <p className="text-xs text-center text-muted-foreground">
                        Didn't receive? <button onClick={handleSendOTP} className="text-primary hover:underline">Resend OTP</button>
                      </p>
                    </div>
                  )}
                </TabsContent>
              </Tabs>
            </CardContent>
          </Card>

          <div className="text-center space-y-3">
            <p className="text-sm text-muted-foreground">
              New to MarketHub?{" "}
              <Link to="/signup" className="text-primary font-medium hover:underline">Create an account</Link>
            </p>
            <Separator />
            <Link to="/vendor/register" className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-primary transition-colors">
              <ShieldCheck className="h-4 w-4" /> Sell on MarketHub <ChevronRight className="h-3 w-3" />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
