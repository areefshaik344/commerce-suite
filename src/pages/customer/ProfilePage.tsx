import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { User as UserIcon, MapPin, Bell, Shield, Settings as SettingsIcon, Building2, ShieldCheck, Heart, Package } from "lucide-react";
import { Link } from "react-router-dom";
import { useProfile } from "@/hooks/useProfile";
import { PersonalDetailsForm } from "@/components/profile/PersonalDetailsForm";
import { AddressManager } from "@/components/profile/AddressManager";
import { ChangePasswordForm } from "@/components/profile/ChangePasswordForm";
import { SessionsManager } from "@/components/profile/SessionsManager";
import { NotificationPreferencesForm } from "@/components/profile/NotificationPreferencesForm";
import { AccountSettingsPanel } from "@/components/profile/AccountSettingsPanel";
import { VendorProfileSection } from "@/components/profile/VendorProfileSection";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

export default function ProfilePage() {
  const { profile, role, isLoading } = useProfile();

  if (isLoading || !profile) {
    return (
      <div className="container py-6 max-w-4xl space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  const isVendor = role === "vendor";
  const isAdmin = role === "admin";

  return (
    <div className="container py-6 max-w-4xl">
      <header className="flex flex-wrap items-center justify-between gap-3 mb-6">
        <div>
          <h1 className="font-display text-xl font-bold">My Account</h1>
          <p className="text-xs text-muted-foreground">
            Member since {new Date(profile.joinedDate).toLocaleDateString("en-IN", { month: "long", year: "numeric" })}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {isAdmin && <Badge className="bg-primary/10 text-primary border-0 gap-1"><ShieldCheck className="h-3 w-3" />Administrator</Badge>}
          {isVendor && <Badge className="bg-success/10 text-success border-0 gap-1"><Building2 className="h-3 w-3" />Verified Seller</Badge>}
          {profile.status === "deactivated" && <Badge variant="outline" className="text-warning border-warning/40">Deactivated</Badge>}
        </div>
      </header>

      {role === "customer" && <CustomerShortcuts />}

      <Tabs defaultValue="profile">
        <TabsList className="mb-6 flex flex-wrap h-auto">
          <TabsTrigger value="profile" className="gap-1"><UserIcon className="h-3 w-3" />Profile</TabsTrigger>
          <TabsTrigger value="addresses" className="gap-1"><MapPin className="h-3 w-3" />Addresses</TabsTrigger>
          <TabsTrigger value="security" className="gap-1"><Shield className="h-3 w-3" />Security</TabsTrigger>
          <TabsTrigger value="notifications" className="gap-1"><Bell className="h-3 w-3" />Notifications</TabsTrigger>
          {(isVendor || profile.vendorStatus === "pending") && (
            <TabsTrigger value="business" className="gap-1"><Building2 className="h-3 w-3" />Business</TabsTrigger>
          )}
          <TabsTrigger value="settings" className="gap-1"><SettingsIcon className="h-3 w-3" />Settings</TabsTrigger>
        </TabsList>

        <TabsContent value="profile"><PersonalDetailsForm /></TabsContent>
        <TabsContent value="addresses"><AddressManager /></TabsContent>
        <TabsContent value="security" className="space-y-4">
          <ChangePasswordForm />
          <SessionsManager />
        </TabsContent>
        <TabsContent value="notifications"><NotificationPreferencesForm /></TabsContent>
        {(isVendor || profile.vendorStatus === "pending") && (
          <TabsContent value="business"><VendorProfileSection /></TabsContent>
        )}
        <TabsContent value="settings"><AccountSettingsPanel /></TabsContent>
      </Tabs>
    </div>
  );
}

function CustomerShortcuts() {
  return (
    <div className="grid grid-cols-2 md:grid-cols-3 gap-3 mb-6">
      <ShortcutCard to="/orders" icon={Package} label="My Orders" desc="Track and manage purchases" />
      <ShortcutCard to="/wishlist" icon={Heart} label="Wishlist" desc="Saved items" />
      <ShortcutCard to="/notifications" icon={Bell} label="Notifications" desc="Activity feed" />
    </div>
  );
}

function ShortcutCard({ to, icon: Icon, label, desc }: { to: string; icon: typeof Heart; label: string; desc: string }) {
  return (
    <Link to={to}>
      <Card className="shadow-card hover:shadow-md transition-shadow">
        <CardContent className="p-4 flex items-center gap-3">
          <div className="h-9 w-9 rounded-full bg-primary/10 grid place-items-center">
            <Icon className="h-4 w-4 text-primary" />
          </div>
          <div>
            <p className="text-sm font-medium">{label}</p>
            <p className="text-[11px] text-muted-foreground">{desc}</p>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
