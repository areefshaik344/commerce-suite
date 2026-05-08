import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Store, ArrowLeft, TrendingUp, Shield, Zap, IndianRupee } from "lucide-react";
import { useStore } from "@/store/useStore";
import { useToast } from "@/hooks/use-toast";

export default function VendorRegisterPage() {
  const [storeName, setStoreName] = useState("");
  const [category, setCategory] = useState("");
  const [description, setDescription] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [loading, setLoading] = useState(false);

  const { applyAsVendor, currentUser, isAuthenticated } = useStore();
  const navigate = useNavigate();
  const { toast } = useToast();

  // Auth + status guard: customer-as-applicant model
  useEffect(() => {
    if (!isAuthenticated) {
      navigate("/login", { state: { from: "/vendor/register" }, replace: true });
      return;
    }
    const status = currentUser?.vendorStatus ?? "none";
    if (currentUser?.isVendor || status === "active" || status === "approved") {
      navigate("/vendor", { replace: true });
    } else if (status === "pending") {
      navigate("/vendor/register/success", { replace: true });
    }
  }, [isAuthenticated, currentUser, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!agreed) {
      toast({ title: "Accept terms", description: "Please agree to the vendor terms.", variant: "destructive" });
      return;
    }
    setLoading(true);
    await new Promise(r => setTimeout(r, 900));
    const res = applyAsVendor(storeName, category, description);
    setLoading(false);
    if (!res.success) {
      toast({ title: "Couldn't submit", description: res.message, variant: "destructive" });
      return;
    }
    toast({ title: "Application submitted!", description: "We'll review within 2-3 business days." });
    navigate("/vendor/register/success");
  };

  return (
    <div className="min-h-screen bg-background flex">
      {/* Left panel */}
      <div className="hidden lg:flex lg:w-[480px] gradient-primary flex-col justify-between p-10 text-primary-foreground relative overflow-hidden">
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-20 -left-10 w-60 h-60 rounded-full bg-white/20 blur-3xl" />
          <div className="absolute bottom-20 right-0 w-80 h-80 rounded-full bg-white/10 blur-3xl" />
        </div>
        <div className="relative z-10">
          <Link to="/">
            <h1 className="text-3xl font-display font-bold">MarketHub</h1>
          </Link>
          <p className="text-sm mt-1 opacity-80">Sell to millions across India</p>
        </div>
        <div className="relative z-10 space-y-5">
          <p className="text-lg font-display font-semibold">Why sell on MarketHub?</p>
          <div className="space-y-4">
            {[
              { icon: TrendingUp, title: "89K+ Active Customers", desc: "Reach a massive buyer base" },
              { icon: IndianRupee, title: "₹256Cr+ Revenue", desc: "Marketplace-level transactions" },
              { icon: Shield, title: "Secure Payments", desc: "7-day settlement guarantee" },
              { icon: Zap, title: "Easy Dashboard", desc: "Manage inventory, orders & analytics" },
            ].map(item => (
              <div key={item.title} className="flex items-start gap-3">
                <div className="h-10 w-10 rounded-xl bg-white/15 flex items-center justify-center shrink-0">
                  <item.icon className="h-5 w-5" />
                </div>
                <div>
                  <p className="font-semibold text-sm">{item.title}</p>
                  <p className="text-xs opacity-70">{item.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
        <p className="relative z-10 text-xs opacity-60">© 2025 MarketHub. All rights reserved.</p>
      </div>

      {/* Right form */}
      <div className="flex-1 flex items-center justify-center p-6 overflow-auto">
        <div className="w-full max-w-lg space-y-6">
          <div>
            <Link to="/" className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors mb-4">
              <ArrowLeft className="h-4 w-4" /> Back to MarketHub
            </Link>
            <h2 className="text-2xl font-display font-bold flex items-center gap-2">
              <Store className="h-6 w-6 text-primary" /> Become a Seller
            </h2>
            <p className="text-sm text-muted-foreground mt-1">
              Applying as <span className="font-medium text-foreground">{currentUser?.name}</span> ({currentUser?.email})
            </p>
          </div>

          <Card className="shadow-elevated border-0">
            <CardContent className="p-6">
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="space-y-1">
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Store Details</p>
                  <p className="text-xs text-muted-foreground">Tell us about the store you want to open. You can complete KYC, bank & pickup details after approval.</p>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Store Name *</Label>
                    <Input placeholder="e.g. TechZone Electronics" value={storeName} onChange={e => setStoreName(e.target.value)} required minLength={3} maxLength={80} />
                  </div>
                  <div className="space-y-2">
                    <Label>Primary Category *</Label>
                    <Select value={category} onValueChange={setCategory} required>
                      <SelectTrigger><SelectValue placeholder="Select" /></SelectTrigger>
                      <SelectContent>
                        {["Electronics", "Fashion", "Home & Living", "Beauty", "Sports", "Books", "Groceries", "Toys & Games"].map(c => (
                          <SelectItem key={c} value={c.toLowerCase()}>{c}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                <div className="space-y-2">
                  <Label>Store Description *</Label>
                  <Textarea placeholder="What will you sell? What makes your store unique?" value={description} onChange={e => setDescription(e.target.value)} rows={3} required minLength={20} maxLength={500} />
                  <p className="text-[11px] text-muted-foreground text-right">{description.length}/500</p>
                </div>

                <div className="rounded-lg bg-muted/50 p-3 text-xs text-muted-foreground">
                  <p className="font-medium text-foreground mb-1">What happens next?</p>
                  <ol className="list-decimal list-inside space-y-0.5">
                    <li>We review your application (2-3 business days)</li>
                    <li>Once approved, complete KYC & bank details</li>
                    <li>Start listing products and selling!</li>
                  </ol>
                </div>

                <div className="flex items-start gap-2">
                  <Checkbox id="vendor-terms" checked={agreed} onCheckedChange={(v) => setAgreed(v === true)} className="mt-0.5" />
                  <label htmlFor="vendor-terms" className="text-xs text-muted-foreground leading-relaxed">
                    I agree to the <Link to="/" className="text-primary hover:underline">Vendor Agreement</Link>,{" "}
                    <Link to="/" className="text-primary hover:underline">Commission Policy</Link>, and{" "}
                    <Link to="/" className="text-primary hover:underline">Terms of Service</Link>
                  </label>
                </div>

                <Button type="submit" className="w-full" disabled={loading}>
                  {loading ? "Submitting application..." : "Submit Application"}
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
