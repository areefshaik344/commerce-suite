import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Megaphone, Plus, TrendingUp, Eye, MousePointerClick, IndianRupee } from "lucide-react";
import { StatCard } from "@/components/shared/StatCard";
import { useToast } from "@/hooks/use-toast";

interface Campaign {
  id: string;
  name: string;
  type: "sponsored_product" | "banner" | "search";
  status: "active" | "paused" | "ended";
  budgetDaily: number;
  spent: number;
  impressions: number;
  clicks: number;
  conversions: number;
}

const initial: Campaign[] = [
  { id: "ad-1", name: "Diwali Mega Sale", type: "sponsored_product", status: "active", budgetDaily: 2000, spent: 14380, impressions: 184320, clicks: 5210, conversions: 312 },
  { id: "ad-2", name: "Wireless Earbuds Push", type: "search", status: "active", budgetDaily: 800, spent: 6420, impressions: 89210, clicks: 2810, conversions: 184 },
  { id: "ad-3", name: "Homepage Banner – Festive", type: "banner", status: "paused", budgetDaily: 5000, spent: 22000, impressions: 412000, clicks: 8200, conversions: 410 },
];

export default function VendorAds() {
  const [campaigns, setCampaigns] = useState<Campaign[]>(initial);
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [type, setType] = useState<Campaign["type"]>("sponsored_product");
  const [budget, setBudget] = useState("500");
  const { toast } = useToast();

  const totalSpend = campaigns.reduce((s, c) => s + c.spent, 0);
  const totalImpr = campaigns.reduce((s, c) => s + c.impressions, 0);
  const totalClicks = campaigns.reduce((s, c) => s + c.clicks, 0);
  const ctr = totalImpr ? ((totalClicks / totalImpr) * 100).toFixed(2) : "0";

  const create = () => {
    if (!name) return;
    setCampaigns((c) => [
      { id: `ad-${Date.now()}`, name, type, status: "active", budgetDaily: parseInt(budget) || 500, spent: 0, impressions: 0, clicks: 0, conversions: 0 },
      ...c,
    ]);
    toast({ title: "Campaign created", description: `${name} is now live.` });
    setOpen(false);
    setName("");
    setBudget("500");
  };

  const toggle = (id: string) => {
    setCampaigns((cs) => cs.map((c) => c.id === id ? { ...c, status: c.status === "active" ? "paused" : "active" } : c));
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-xl font-bold">Ads & Promotions</h1>
          <p className="text-sm text-muted-foreground">Boost product visibility with sponsored placements.</p>
        </div>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild><Button className="gap-1.5"><Plus className="h-4 w-4" /> New campaign</Button></DialogTrigger>
          <DialogContent>
            <DialogHeader><DialogTitle>Create campaign</DialogTitle></DialogHeader>
            <div className="space-y-4">
              <div className="space-y-2"><Label>Campaign name</Label><Input value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Summer Sale Push" /></div>
              <div className="space-y-2">
                <Label>Type</Label>
                <Select value={type} onValueChange={(v) => setType(v as Campaign["type"])}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="sponsored_product">Sponsored Product</SelectItem>
                    <SelectItem value="search">Search Ads</SelectItem>
                    <SelectItem value="banner">Homepage Banner</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2"><Label>Daily budget (₹)</Label><Input type="number" value={budget} onChange={(e) => setBudget(e.target.value)} /></div>
              <Button onClick={create} className="w-full">Launch campaign</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <StatCard title="Total Spend" value={`₹${(totalSpend / 1000).toFixed(1)}K`} icon={IndianRupee} iconClassName="bg-primary/10 text-primary" />
        <StatCard title="Impressions" value={totalImpr.toLocaleString("en-IN")} icon={Eye} iconClassName="bg-secondary/10 text-secondary" />
        <StatCard title="Clicks" value={totalClicks.toLocaleString("en-IN")} icon={MousePointerClick} iconClassName="bg-accent/10 text-accent-foreground" />
        <StatCard title="CTR" value={`${ctr}%`} icon={TrendingUp} iconClassName="bg-success/10 text-success" />
      </div>

      <Card className="shadow-card">
        <CardHeader className="pb-2"><CardTitle className="text-sm">Active campaigns</CardTitle></CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-muted/40 text-xs">
                <tr className="text-left text-muted-foreground">
                  <th className="px-4 py-2 font-medium">Campaign</th>
                  <th className="px-4 py-2 font-medium">Type</th>
                  <th className="px-4 py-2 font-medium text-right">Budget/day</th>
                  <th className="px-4 py-2 font-medium text-right">Spent</th>
                  <th className="px-4 py-2 font-medium text-right">CTR</th>
                  <th className="px-4 py-2 font-medium text-right">Conv.</th>
                  <th className="px-4 py-2 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {campaigns.map((c) => (
                  <tr key={c.id} className="border-t hover:bg-muted/40">
                    <td className="px-4 py-3 font-medium flex items-center gap-2"><Megaphone className="h-3.5 w-3.5 text-primary" />{c.name}</td>
                    <td className="px-4 py-3 text-muted-foreground capitalize">{c.type.replace("_", " ")}</td>
                    <td className="px-4 py-3 text-right">₹{c.budgetDaily.toLocaleString("en-IN")}</td>
                    <td className="px-4 py-3 text-right">₹{c.spent.toLocaleString("en-IN")}</td>
                    <td className="px-4 py-3 text-right">{c.impressions ? ((c.clicks / c.impressions) * 100).toFixed(2) : "0"}%</td>
                    <td className="px-4 py-3 text-right">{c.conversions}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <Switch checked={c.status === "active"} onCheckedChange={() => toggle(c.id)} />
                        <Badge variant="secondary" className="text-xs capitalize">{c.status}</Badge>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}