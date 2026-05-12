import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "sonner";
import { useProfile } from "@/hooks/useProfile";
import type { NotificationPreferences } from "@/data/mock-users";

const GROUPS: { title: string; items: { key: keyof NotificationPreferences; label: string; description: string }[] }[] = [
  {
    title: "Email",
    items: [
      { key: "emailOrders", label: "Order updates", description: "Order confirmations, shipping and delivery" },
      { key: "emailPromotions", label: "Promotions & deals", description: "Personalised offers and price drops" },
      { key: "emailNewsletter", label: "Newsletter", description: "Monthly highlights from the marketplace" },
    ],
  },
  {
    title: "SMS",
    items: [
      { key: "smsOrders", label: "Order updates", description: "Critical delivery alerts via SMS" },
      { key: "smsPromotions", label: "Promotions", description: "Marketing SMS (we keep this rare)" },
    ],
  },
  {
    title: "Push & Messaging",
    items: [
      { key: "pushOrders", label: "Push: orders", description: "Real-time push for order events" },
      { key: "pushPromotions", label: "Push: promotions", description: "Deals and recommendations" },
      { key: "whatsappOrders", label: "WhatsApp: orders", description: "Get delivery updates on WhatsApp" },
    ],
  },
];

export function NotificationPreferencesForm() {
  const { preferences, updatePreferences, isLoading, savingScope } = useProfile();
  const isSaving = savingScope === "preferences";

  if (isLoading || !preferences) {
    return <div className="space-y-3">{[1, 2, 3].map((i) => <Skeleton key={i} className="h-20 w-full" />)}</div>;
  }

  const onToggle = async (key: keyof NotificationPreferences, val: boolean) => {
    try { await updatePreferences({ [key]: val } as Partial<NotificationPreferences>); }
    catch (e) { toast.error(e instanceof Error ? e.message : "Could not save preference"); }
  };

  return (
    <div className="space-y-4">
      {GROUPS.map((g) => (
        <Card key={g.title} className="shadow-card">
          <CardHeader className="pb-3"><CardTitle className="text-base">{g.title}</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            {g.items.map((item) => (
              <div key={String(item.key)} className="flex items-center justify-between gap-4">
                <div>
                  <p className="text-sm font-medium">{item.label}</p>
                  <p className="text-xs text-muted-foreground">{item.description}</p>
                </div>
                <Switch
                  checked={Boolean(preferences[item.key])}
                  onCheckedChange={(v) => onToggle(item.key, v)}
                  disabled={isSaving}
                />
              </div>
            ))}
          </CardContent>
        </Card>
      ))}
    </div>
  );
}