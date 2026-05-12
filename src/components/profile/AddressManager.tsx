import { useState } from "react";
import { useFormik } from "formik";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from "@/components/ui/dialog";
import { Loader2, MapPin, Plus, Star } from "lucide-react";
import { toast } from "sonner";
import { useProfile } from "@/hooks/useProfile";
import { addressSchema } from "@/lib/profileValidation";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import type { Address } from "@/data/mock-users";
import { EmptyState } from "@/components/shared/EmptyState";
import { Skeleton } from "@/components/ui/skeleton";

const STATES = [
  "Andhra Pradesh", "Karnataka", "Maharashtra", "Tamil Nadu", "Delhi",
  "Gujarat", "Rajasthan", "West Bengal", "Kerala", "Uttar Pradesh",
];
const TYPES: Address["type"][] = ["HOME", "WORK", "OTHER"];

function deriveType(label: string): Address["type"] {
  const l = label.toLowerCase();
  if (l.includes("home")) return "HOME";
  if (l.includes("office") || l.includes("work")) return "WORK";
  return "OTHER";
}

export function AddressManager() {
  const { addresses, addAddress, updateAddress, deleteAddress, setDefaultAddress, savingScope, isLoading } = useProfile();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Address | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);
  const isSaving = savingScope === "address";

  const onAdd = () => { setEditing(null); setOpen(true); };
  const onEdit = (a: Address) => { setEditing(a); setOpen(true); };

  const onConfirmDelete = async () => {
    if (!confirmDelete) return;
    try { await deleteAddress(confirmDelete); toast.success("Address removed"); }
    catch (err) { toast.error(err instanceof Error ? err.message : "Could not remove address"); }
    finally { setConfirmDelete(null); }
  };

  if (isLoading) {
    return <div className="space-y-3">{[1, 2].map((i) => <Skeleton key={i} className="h-32 w-full" />)}</div>;
  }

  return (
    <div className="space-y-3">
      {addresses.length === 0 ? (
        <EmptyState
          icon={MapPin} title="No addresses yet"
          description="Add a delivery address to speed up checkout."
          action={<Button onClick={onAdd} className="gap-1"><Plus className="h-4 w-4" />Add address</Button>}
        />
      ) : (
        <>
          {addresses.map((a) => (
            <Card key={a.id} className="shadow-card">
              <CardContent className="p-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-medium text-sm">{a.name}</span>
                      <Badge variant="outline" className="text-[10px]">{a.type ?? deriveType(a.label)}</Badge>
                      {a.isDefault && <Badge className="text-[10px] bg-success/10 text-success border-0 gap-1"><Star className="h-3 w-3" />Default</Badge>}
                    </div>
                    <p className="text-sm text-muted-foreground">{a.line1}{a.line2 ? `, ${a.line2}` : ""}</p>
                    <p className="text-sm text-muted-foreground">{a.city}, {a.state} - {a.pincode}</p>
                    <p className="text-xs text-muted-foreground mt-1">📞 {a.phone}</p>
                  </div>
                </div>
                <div className="flex flex-wrap gap-2 mt-3">
                  <Button variant="outline" size="sm" className="text-xs" onClick={() => onEdit(a)}>Edit</Button>
                  {!a.isDefault && (
                    <Button
                      variant="outline" size="sm" className="text-xs gap-1"
                      disabled={isSaving}
                      onClick={async () => {
                        try { await setDefaultAddress(a.id); toast.success("Default updated"); }
                        catch (e) { toast.error(e instanceof Error ? e.message : "Failed"); }
                      }}
                    >
                      <Star className="h-3 w-3" />Make default
                    </Button>
                  )}
                  <Button
                    variant="ghost" size="sm" className="text-xs text-destructive"
                    onClick={() => setConfirmDelete(a.id)}
                    disabled={isSaving}
                  >
                    Remove
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
          <Button variant="outline" className="w-full gap-1" onClick={onAdd}><Plus className="h-4 w-4" />Add new address</Button>
        </>
      )}

      <AddressDialog
        open={open} onOpenChange={setOpen} address={editing}
        onSubmit={async (values) => {
          try {
            if (editing) {
              await updateAddress(editing.id, values);
              toast.success("Address updated");
            } else {
              await addAddress(values);
              toast.success("Address added");
            }
            setOpen(false);
          } catch (e) {
            toast.error(e instanceof Error ? e.message : "Save failed");
          }
        }}
      />

      <ConfirmDialog
        open={!!confirmDelete} onOpenChange={(v) => !v && setConfirmDelete(null)}
        title="Remove this address?" description="This action can't be undone."
        confirmLabel="Remove" variant="destructive" onConfirm={onConfirmDelete}
      />
    </div>
  );
}

function AddressDialog({
  open, onOpenChange, address, onSubmit,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  address: Address | null;
  onSubmit: (a: Omit<Address, "id">) => Promise<void>;
}) {
  const formik = useFormik({
    enableReinitialize: true,
    initialValues: {
      name: address?.name ?? "",
      phone: address?.phone ?? "",
      line1: address?.line1 ?? "",
      line2: address?.line2 ?? "",
      city: address?.city ?? "",
      state: address?.state ?? "",
      pincode: address?.pincode ?? "",
      type: (address?.type ?? deriveType(address?.label ?? "Home")) as Address["type"],
      isDefault: address?.isDefault ?? false,
    },
    validationSchema: addressSchema,
    onSubmit: async (v) => {
      const labelMap: Record<NonNullable<Address["type"]>, string> = { HOME: "Home", WORK: "Work", OTHER: "Other" };
      await onSubmit({
        name: v.name, phone: v.phone, line1: v.line1, line2: v.line2,
        city: v.city, state: v.state, pincode: v.pincode,
        type: v.type, label: labelMap[v.type ?? "OTHER"], isDefault: v.isDefault,
      });
    },
  });

  const F = ({ name, label, ...rest }: { name: keyof typeof formik.values; label: string } & React.InputHTMLAttributes<HTMLInputElement>) => {
    const err = formik.touched[name] && formik.errors[name];
    return (
      <div>
        <Label className="text-xs">{label}</Label>
        <Input
          name={name as string} value={String(formik.values[name] ?? "")}
          onChange={formik.handleChange} onBlur={formik.handleBlur}
          className="mt-1" {...rest}
        />
        {err && <p className="text-[11px] text-destructive mt-1">{String(err)}</p>}
      </div>
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>{address ? "Edit Address" : "Add New Address"}</DialogTitle>
          <DialogDescription>Used for shipping and billing on this account.</DialogDescription>
        </DialogHeader>
        <form onSubmit={formik.handleSubmit} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <F name="name" label="Full Name" />
            <F name="phone" label="Phone" />
          </div>
          <F name="line1" label="Address Line 1" placeholder="House / Flat / Office No." />
          <F name="line2" label="Address Line 2" placeholder="Street, Landmark" />
          <div className="grid grid-cols-3 gap-3">
            <F name="city" label="City" />
            <div>
              <Label className="text-xs">State</Label>
              <Select value={formik.values.state} onValueChange={(v) => formik.setFieldValue("state", v)}>
                <SelectTrigger className="mt-1"><SelectValue placeholder="Select" /></SelectTrigger>
                <SelectContent>
                  {STATES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
                </SelectContent>
              </Select>
              {formik.touched.state && formik.errors.state && <p className="text-[11px] text-destructive mt-1">{formik.errors.state}</p>}
            </div>
            <F name="pincode" label="Pincode" maxLength={6} />
          </div>
          <div className="flex items-center gap-4 pt-1">
            <div>
              <Label className="text-xs">Type</Label>
              <Select value={formik.values.type ?? "OTHER"} onValueChange={(v) => formik.setFieldValue("type", v)}>
                <SelectTrigger className="mt-1 w-32"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {TYPES.map((t) => <SelectItem key={t} value={t!}>{t}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <label className="flex items-center gap-2 mt-5 text-sm cursor-pointer">
              <Checkbox checked={formik.values.isDefault} onCheckedChange={(v) => formik.setFieldValue("isDefault", v === true)} />
              Set as default
            </label>
          </div>
          <DialogFooter>
            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={formik.isSubmitting || !formik.isValid} className="gap-1">
              {formik.isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
              {address ? "Update" : "Save"} address
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}