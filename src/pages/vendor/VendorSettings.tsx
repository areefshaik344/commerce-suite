import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { vendorApi } from "@/api/vendorApi";
import { Skeleton } from "@/components/ui/skeleton";

export default function VendorSettings() {
  const { toast } = useToast();
  const qc = useQueryClient();

  const profileQ = useQuery({
    queryKey: ["vendor", "me", "profile"],
    queryFn: () => vendorApi.myProfile().then(r => r.data),
  });

  const [storeName, setStoreName] = useState("");
  const [description, setDescription] = useState("");
  const [supportEmail, setSupportEmail] = useState("");
  const [supportPhone, setSupportPhone] = useState("");
  const [returnPolicy, setReturnPolicy] = useState("");

  useEffect(() => {
    if (profileQ.data) {
      setStoreName(profileQ.data.storeName ?? "");
      setDescription(profileQ.data.description ?? "");
      setSupportEmail(profileQ.data.supportEmail ?? "");
      setSupportPhone(profileQ.data.supportPhone ?? "");
      setReturnPolicy(profileQ.data.returnPolicy ?? "");
    }
  }, [profileQ.data]);

  const saveM = useMutation({
    mutationFn: () => vendorApi.updateProfileV2({
      storeName, description, supportEmail, supportPhone, returnPolicy,
    }),
    onSuccess: () => {
      toast({ title: "Settings saved", description: "Your store settings have been updated." });
      qc.invalidateQueries({ queryKey: ["vendor", "me", "profile"] });
    },
    onError: (e: unknown) => toast({ title: "Save failed", description: (e as Error).message, variant: "destructive" }),
  });

  if (profileQ.isLoading) {
    return <div className="space-y-3 max-w-2xl"><Skeleton className="h-8 w-1/2" /><Skeleton className="h-64" /></div>;
  }

  if (profileQ.isError) {
    return (
      <div className="max-w-2xl">
        <Card className="shadow-card"><CardContent className="p-8 text-center text-sm text-muted-foreground">
          Vendor profile is not available yet. Complete onboarding to access settings.
        </CardContent></Card>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-2xl">
      <h1 className="font-display text-xl font-bold">Store Settings</h1>

      <Card className="shadow-card">
        <CardHeader><CardTitle className="text-base">Store Information</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div><Label className="text-xs">Store Name</Label><Input value={storeName} onChange={e => setStoreName(e.target.value)} className="mt-1" /></div>
          <div><Label className="text-xs">Description</Label><Textarea value={description} onChange={e => setDescription(e.target.value)} className="mt-1" rows={3} /></div>
          <div className="grid grid-cols-2 gap-4">
            <div><Label className="text-xs">Support Email</Label><Input type="email" value={supportEmail} onChange={e => setSupportEmail(e.target.value)} className="mt-1" /></div>
            <div><Label className="text-xs">Support Phone</Label><Input value={supportPhone} onChange={e => setSupportPhone(e.target.value)} className="mt-1" /></div>
          </div>
        </CardContent>
      </Card>

      <Card className="shadow-card">
        <CardHeader><CardTitle className="text-base">Return Policy</CardTitle></CardHeader>
        <CardContent>
          <Textarea value={returnPolicy} onChange={e => setReturnPolicy(e.target.value)} rows={3} />
        </CardContent>
      </Card>

      <Button onClick={() => saveM.mutate()} disabled={saveM.isPending} className="w-full">
        {saveM.isPending ? "Saving..." : "Save Changes"}
      </Button>
    </div>
  );
}