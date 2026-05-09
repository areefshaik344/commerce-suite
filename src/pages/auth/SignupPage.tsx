import { Link, useNavigate } from "react-router-dom";
import { Formik, Form } from "formik";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Separator } from "@/components/ui/separator";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { ShieldCheck, ChevronRight, AlertCircle, Loader2 } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { toast } from "sonner";
import PasswordInput from "@/components/auth/PasswordInput";
import PasswordStrengthMeter from "@/components/auth/PasswordStrengthMeter";
import { signupSchema } from "@/lib/validation";
import { ApiError } from "@/api/apiClient";
import { useState } from "react";

export default function SignupPage() {
  const { signupWithCredentials } = useAuth();
  const navigate = useNavigate();
  const [globalError, setGlobalError] = useState<string | null>(null);

  return (
    <div className="min-h-screen bg-background flex">
      <div className="hidden lg:flex lg:w-[480px] gradient-primary flex-col justify-between p-10 text-primary-foreground relative overflow-hidden">
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-20 -left-10 w-60 h-60 rounded-full bg-white/20 blur-3xl" />
          <div className="absolute bottom-20 right-0 w-80 h-80 rounded-full bg-white/10 blur-3xl" />
        </div>
        <div className="relative z-10">
          <Link to="/"><h1 className="text-3xl font-display font-bold">MarketHub</h1></Link>
          <p className="text-sm mt-1 opacity-80">Join millions of happy shoppers</p>
        </div>
        <div className="relative z-10 space-y-4">
          <div className="bg-white/10 rounded-xl p-5 backdrop-blur-sm">
            <p className="text-sm font-medium mb-3">Why join MarketHub?</p>
            <ul className="space-y-2 text-xs opacity-90">
              <li className="flex items-center gap-2">✓ Access 10,000+ verified sellers</li>
              <li className="flex items-center gap-2">✓ Track orders in real-time</li>
              <li className="flex items-center gap-2">✓ Exclusive member-only deals</li>
              <li className="flex items-center gap-2">✓ Easy 7-day returns</li>
              <li className="flex items-center gap-2">✓ Secure payments with buyer protection</li>
            </ul>
          </div>
        </div>
        <p className="relative z-10 text-xs opacity-60">© 2025 MarketHub. All rights reserved.</p>
      </div>

      <div className="flex-1 flex items-center justify-center p-6">
        <div className="w-full max-w-md space-y-6">
          <div className="text-center lg:text-left">
            <h2 className="text-2xl font-display font-bold">Create your account</h2>
            <p className="text-sm text-muted-foreground mt-1">Start shopping in under a minute</p>
          </div>

          <Card className="shadow-elevated border-0">
            <CardContent className="p-6">
              {globalError && (
                <Alert variant="destructive" className="mb-3">
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>{globalError}</AlertDescription>
                </Alert>
              )}
              <Formik
                initialValues={{ name: "", email: "", phone: "", password: "", confirmPassword: "", acceptTerms: false }}
                validationSchema={signupSchema}
                onSubmit={async (values, { setSubmitting }) => {
                  setGlobalError(null);
                  try {
                    await signupWithCredentials(values.name, values.email.trim(), values.phone, values.password);
                    toast.success("Account created!", { description: "Please verify your email." });
                    navigate("/verify-email", { state: { email: values.email.trim() } });
                  } catch (err) {
                    const message = err instanceof ApiError ? err.message : "Could not create account";
                    setGlobalError(message);
                  } finally {
                    setSubmitting(false);
                  }
                }}
              >
                {({ values, errors, touched, handleChange, handleBlur, setFieldValue, isSubmitting }) => (
                  <Form className="space-y-4" noValidate>
                    <Field label="Full Name" name="name" placeholder="Rahul Sharma"
                      value={values.name} error={touched.name ? errors.name : undefined}
                      onChange={handleChange} onBlur={handleBlur} autoComplete="name" />
                    <Field label="Email Address" name="email" type="email" placeholder="rahul@example.com"
                      value={values.email} error={touched.email ? errors.email : undefined}
                      onChange={handleChange} onBlur={handleBlur} autoComplete="email" />
                    <div className="space-y-2">
                      <Label htmlFor="phone">Phone Number</Label>
                      <div className="flex gap-2">
                        <div className="flex items-center px-3 rounded-md border bg-muted text-sm font-medium text-muted-foreground">+91</div>
                        <Input id="phone" name="phone" placeholder="98765 43210" value={values.phone}
                          inputMode="numeric" maxLength={10} autoComplete="tel-national"
                          onChange={(e) => setFieldValue("phone", e.target.value.replace(/\D/g, "").slice(0, 10))}
                          onBlur={handleBlur}
                          aria-invalid={!!(touched.phone && errors.phone)} />
                      </div>
                      {touched.phone && errors.phone && <p className="text-xs text-destructive">{errors.phone}</p>}
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="password">Password</Label>
                      <PasswordInput id="password" name="password" autoComplete="new-password"
                        placeholder="Min 8 chars, mix of cases, number, symbol"
                        value={values.password} onChange={handleChange} onBlur={handleBlur}
                        invalid={!!(touched.password && errors.password)} />
                      <PasswordStrengthMeter password={values.password} />
                      {touched.password && errors.password && <p className="text-xs text-destructive">{errors.password}</p>}
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="confirmPassword">Confirm Password</Label>
                      <PasswordInput id="confirmPassword" name="confirmPassword" autoComplete="new-password"
                        placeholder="Re-enter password"
                        value={values.confirmPassword} onChange={handleChange} onBlur={handleBlur}
                        invalid={!!(touched.confirmPassword && errors.confirmPassword)} />
                      {touched.confirmPassword && errors.confirmPassword && (
                        <p className="text-xs text-destructive">{errors.confirmPassword}</p>
                      )}
                    </div>
                    <div className="flex items-start gap-2">
                      <Checkbox id="acceptTerms" checked={values.acceptTerms}
                        onCheckedChange={(v) => setFieldValue("acceptTerms", v === true)} className="mt-0.5" />
                      <label htmlFor="acceptTerms" className="text-xs text-muted-foreground leading-relaxed">
                        I agree to the <Link to="/terms" className="text-primary hover:underline">Terms of Service</Link> and{" "}
                        <Link to="/privacy" className="text-primary hover:underline">Privacy Policy</Link>
                      </label>
                    </div>
                    {touched.acceptTerms && errors.acceptTerms && (
                      <p className="text-xs text-destructive">{errors.acceptTerms}</p>
                    )}
                    <Button type="submit" className="w-full" disabled={isSubmitting}>
                      {isSubmitting ? <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Creating account...</> : "Create Account"}
                    </Button>
                  </Form>
                )}
              </Formik>
            </CardContent>
          </Card>

          <div className="text-center space-y-3">
            <p className="text-sm text-muted-foreground">
              Already have an account?{" "}
              <Link to="/login" className="text-primary font-medium hover:underline">Sign in</Link>
            </p>
            <Separator />
            <Link to="/vendor/register" className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-primary transition-colors">
              <ShieldCheck className="h-4 w-4" /> Start selling on MarketHub <ChevronRight className="h-3 w-3" />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

interface FieldProps {
  label: string;
  name: string;
  value: string;
  error?: string;
  type?: string;
  placeholder?: string;
  autoComplete?: string;
  onChange: React.ChangeEventHandler<HTMLInputElement>;
  onBlur: React.FocusEventHandler<HTMLInputElement>;
}
function Field({ label, name, value, error, type = "text", placeholder, autoComplete, onChange, onBlur }: FieldProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor={name}>{label}</Label>
      <Input id={name} name={name} type={type} placeholder={placeholder} value={value}
        autoComplete={autoComplete} onChange={onChange} onBlur={onBlur} aria-invalid={!!error} />
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}
