import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { useNotificationPreferences } from "@/hooks/useNotifications";
import type {
  DeliveryChannel, NotificationCategory, NotificationPreferenceMatrix,
} from "@/types/notification";
import { toast } from "sonner";

const CATEGORIES: { key: NotificationCategory; label: string; description: string }[] = [
  { key: "order",    label: "Orders",          description: "Confirmations, status changes" },
  { key: "shipping", label: "Shipping",        description: "Dispatch, transit, delivery" },
  { key: "payment",  label: "Payments",        description: "Capture, retry, failure" },
  { key: "refund",   label: "Refunds",         description: "Refund initiated / completed" },
  { key: "return",   label: "Returns",         description: "Return updates" },
  { key: "promo",    label: "Promotions",      description: "Offers, deals (marketing)" },
  { key: "review",   label: "Reviews",         description: "Review reminders" },
];

const CHANNELS: { key: DeliveryChannel; label: string }[] = [
  { key: "in_app", label: "In-app" },
  { key: "email",  label: "Email" },
  { key: "sms",    label: "SMS" },
  { key: "push",   label: "Push" },
];

interface Props { userId: string | undefined }

export function NotificationPreferences({ userId }: Props) {
  const { preferences, loading, update } = useNotificationPreferences(userId);

  if (loading || !preferences) {
    return <div className="space-y-3">{[1, 2, 3].map(i => <Skeleton key={i} className="h-32 w-full" />)}</div>;
  }

  const toggle = async (cat: NotificationCategory, ch: DeliveryChannel, val: boolean) => {
    const matrix: NotificationPreferenceMatrix = {
      ...preferences.matrix,
      [cat]: { ...preferences.matrix[cat], [ch]: val },
    };
    try { await update({ matrix }); }
    catch (e) { toast.error(e instanceof Error ? e.message : "Could not save"); }
  };

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="pb-3"><CardTitle className="text-base">Marketing</CardTitle></CardHeader>
        <CardContent className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium">Receive marketing communications</p>
            <p className="text-xs text-muted-foreground">Deals, promos and product highlights</p>
          </div>
          <Switch
            checked={preferences.marketingOptIn}
            onCheckedChange={(v) => update({ marketingOptIn: v })}
          />
        </CardContent>
      </Card>

      {CATEGORIES.map(cat => (
        <Card key={cat.key}>
          <CardHeader className="pb-3">
            <CardTitle className="text-base">{cat.label}</CardTitle>
            <p className="text-xs text-muted-foreground">{cat.description}</p>
          </CardHeader>
          <CardContent className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {CHANNELS.map(ch => (
              <label key={ch.key} className="flex items-center justify-between gap-2 border rounded-md px-3 py-2">
                <span className="text-sm">{ch.label}</span>
                <Switch
                  checked={Boolean(preferences.matrix[cat.key]?.[ch.key])}
                  onCheckedChange={(v) => toggle(cat.key, ch.key, v)}
                />
              </label>
            ))}
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
