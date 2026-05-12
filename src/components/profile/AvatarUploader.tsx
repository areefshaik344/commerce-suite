import { useRef, useState } from "react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Camera, Loader2, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { useProfile } from "@/hooks/useProfile";
import { AVATAR_ACCEPTED, AVATAR_MAX_BYTES, validateAvatarFile } from "@/lib/profileValidation";
import {
  Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogDescription,
} from "@/components/ui/dialog";

function readAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const r = new FileReader();
    r.onload = () => resolve(String(r.result));
    r.onerror = () => reject(new Error("Could not read file"));
    r.readAsDataURL(file);
  });
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error("Image is corrupt or unsupported"));
    img.src = src;
  });
}

function squareCrop(img: HTMLImageElement, size = 256): string {
  const min = Math.min(img.width, img.height);
  const sx = (img.width - min) / 2;
  const sy = (img.height - min) / 2;
  const c = document.createElement("canvas");
  c.width = size; c.height = size;
  const ctx = c.getContext("2d");
  if (!ctx) return img.src;
  ctx.drawImage(img, sx, sy, min, min, 0, 0, size, size);
  return c.toDataURL("image/jpeg", 0.9);
}

export function AvatarUploader() {
  const { profile, uploadAvatar, removeAvatar, savingScope } = useProfile();
  const inputRef = useRef<HTMLInputElement>(null);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string>("");
  const isSaving = savingScope === "avatar";

  const onPick = () => inputRef.current?.click();

  const onFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    const v = validateAvatarFile(file);
    if (v) { toast.error(v.message); return; }
    try {
      const dataUrl = await readAsDataUrl(file);
      const img = await loadImage(dataUrl);
      const cropped = squareCrop(img, 256);
      setPreviewUrl(cropped);
      setPreviewOpen(true);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Could not process image");
    }
  };

  const confirm = async () => {
    try {
      await uploadAvatar(previewUrl);
      toast.success("Avatar updated");
      setPreviewOpen(false);
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "Upload failed");
    }
  };

  const onRemove = async () => {
    try { await removeAvatar(); toast.success("Avatar removed"); }
    catch (err) { toast.error(err instanceof Error ? err.message : "Could not remove"); }
  };

  if (!profile) return null;

  return (
    <div className="flex items-center gap-4">
      <div className="relative">
        <Avatar className="h-20 w-20 border">
          {profile.avatar ? <AvatarImage src={profile.avatar} alt={profile.name} /> : null}
          <AvatarFallback className="text-xl font-display font-bold text-primary">
            {profile.name.split(" ").map((p) => p[0]).join("").slice(0, 2).toUpperCase()}
          </AvatarFallback>
        </Avatar>
        {isSaving && (
          <div className="absolute inset-0 grid place-items-center rounded-full bg-background/70">
            <Loader2 className="h-5 w-5 animate-spin" />
          </div>
        )}
      </div>
      <div className="flex flex-col gap-2">
        <div className="flex gap-2">
          <Button size="sm" variant="outline" onClick={onPick} disabled={isSaving} className="gap-1">
            <Camera className="h-3 w-3" /> Change photo
          </Button>
          {profile.avatar && (
            <Button size="sm" variant="ghost" onClick={onRemove} disabled={isSaving} className="gap-1 text-destructive">
              <Trash2 className="h-3 w-3" /> Remove
            </Button>
          )}
        </div>
        <p className="text-[11px] text-muted-foreground">JPG, PNG or WEBP. Max {Math.round(AVATAR_MAX_BYTES / 1024 / 1024)} MB.</p>
        <input
          ref={inputRef} type="file" accept={AVATAR_ACCEPTED.join(",")}
          onChange={onFile} className="hidden"
        />
      </div>

      <Dialog open={previewOpen} onOpenChange={setPreviewOpen}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Preview new avatar</DialogTitle>
            <DialogDescription>This is how your photo will appear across the marketplace.</DialogDescription>
          </DialogHeader>
          <div className="flex justify-center py-2">
            {previewUrl && (
              <img src={previewUrl} alt="Preview" className="h-40 w-40 rounded-full object-cover border" />
            )}
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setPreviewOpen(false)}>Cancel</Button>
            <Button onClick={confirm} disabled={isSaving} className="gap-1">
              {isSaving && <Loader2 className="h-4 w-4 animate-spin" />} Save photo
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}