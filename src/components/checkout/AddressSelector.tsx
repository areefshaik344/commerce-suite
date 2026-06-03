import { useState } from "react";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Plus, MapPin } from "lucide-react";
import { AddressFormDialog } from "@/components/shared/AddressFormDialog";
import type { Address } from "@/data/mock-users";

interface Props {
  addresses: Address[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  onAdd?: (addr: Address) => Promise<void> | void;
}

export function AddressSelector({ addresses, selectedId, onSelect, onAdd }: Props) {
  const [open, setOpen] = useState(false);

  if (addresses.length === 0) {
    return (
      <Card><CardContent className="p-6 text-center space-y-3">
        <MapPin className="h-8 w-8 mx-auto text-muted-foreground" />
        <p className="text-sm text-muted-foreground">No saved addresses yet.</p>
        <Button size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4 mr-1" /> Add address</Button>
        {onAdd && <AddressFormDialog open={open} onOpenChange={setOpen} onSave={onAdd} />}
      </CardContent></Card>
    );
  }

  return (
    <div className="space-y-3">
      <RadioGroup value={selectedId ?? ""} onValueChange={onSelect} className="space-y-2">
        {addresses.map(a => (
          <Label key={a.id} htmlFor={`addr-${a.id}`}
                 className="block cursor-pointer rounded-lg border p-3 transition-colors hover:bg-muted/50 has-[:checked]:border-primary has-[:checked]:bg-primary/5">
            <div className="flex items-start gap-3">
              <RadioGroupItem id={`addr-${a.id}`} value={a.id} className="mt-1" />
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-medium text-sm">{a.name}</span>
                  {a.isDefault && <span className="text-[10px] uppercase tracking-wide rounded bg-primary/10 text-primary px-1.5 py-0.5">Default</span>}
                  {a.type && <span className="text-[10px] uppercase tracking-wide rounded bg-muted px-1.5 py-0.5 text-muted-foreground">{a.type}</span>}
                </div>
                <p className="text-xs text-muted-foreground mt-1 leading-relaxed">
                  {a.line1}{a.line2 && `, ${a.line2}`}, {a.city}, {a.state} {a.pincode}
                </p>
                <p className="text-xs text-muted-foreground mt-0.5">{a.phone}</p>
              </div>
            </div>
          </Label>
        ))}
      </RadioGroup>
      {onAdd && (
        <>
          <Button variant="outline" size="sm" onClick={() => setOpen(true)}>
            <Plus className="h-4 w-4 mr-1" /> Add new address
          </Button>
          <AddressFormDialog open={open} onOpenChange={setOpen} onSave={onAdd} />
        </>
      )}
    </div>
  );
}