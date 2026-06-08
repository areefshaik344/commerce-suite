import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { ArrowLeft, Upload, X, Plus, Trash2, GripVertical, ImagePlus, Save } from "lucide-react";
import { toast } from "@/hooks/use-toast";
import { useQuery, useMutation } from "@tanstack/react-query";
import { categoryApi } from "@/api/categoryApi";
import { vendorProductApi } from "@/api/vendorProductApi";
import { paiseToRupees, rupeesToPaise } from "@/lib/money";

interface Variant { name: string; options: string[]; }
interface SpecRow { key: string; value: string; }

export default function VendorProductEdit() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { data: product } = useQuery({
    queryKey: ["vendor", "product", id],
    queryFn: () => vendorProductApi.get(id!).then(r => r.data),
    enabled: !!id,
  });
  const categoriesQ = useQuery({
    queryKey: ["catalog", "categories", "tree"],
    queryFn: () => categoryApi.getCategoryTree().then(r => r.data),
  });
  const categories = (categoriesQ.data ?? []).map(c => ({
    id: c.id, name: c.name, slug: c.slug, icon: c.icon ?? "",
    subcategories: (c.children ?? []).map(ch => ch.name),
  }));

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("");
  const [subcategory, setSubcategory] = useState("");
  const [brand, setBrand] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState("");
  const [price, setPrice] = useState("");
  const [originalPrice, setOriginalPrice] = useState("");
  const [sku, setSku] = useState("");
  const [stockCount, setStockCount] = useState("");
  const [lowStockThreshold, setLowStockThreshold] = useState("10");
  const [trackInventory, setTrackInventory] = useState(true);
  const [images, setImages] = useState<string[]>([]);
  const [variants, setVariants] = useState<Variant[]>([]);
  const [variantName, setVariantName] = useState("");
  const [variantOptionInput, setVariantOptionInput] = useState("");
  const [specs, setSpecs] = useState<SpecRow[]>([]);
  const [featured, setFeatured] = useState(false);

  useEffect(() => {
    if (product) {
      setName(product.title);
      setDescription(product.description ?? "");
      const cat = categories.find(c => c.id === product.categoryId);
      setCategory(cat?.slug || "");
      setPrice(String(paiseToRupees(product.salePricePaise ?? product.basePricePaise)));
      setOriginalPrice(String(paiseToRupees(product.basePricePaise)));
      setImages(product.primaryImageUrl ? [product.primaryImageUrl] : []);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [product, categoriesQ.data]);

  const selectedCategory = categories.find(c => c.slug === category);

  const addTag = () => {
    const t = tagInput.trim();
    if (t && !tags.includes(t)) { setTags([...tags, t]); setTagInput(""); }
  };

  const handleImageUpload = () => {
    const placeholders = [
      "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&h=400&fit=crop",
      "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&h=400&fit=crop",
      "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=400&h=400&fit=crop",
    ];
    if (images.length < 6) setImages([...images, placeholders[images.length % placeholders.length]]);
  };

  const addVariant = () => {
    if (!variantName.trim()) return;
    setVariants([...variants, { name: variantName.trim(), options: [] }]);
    setVariantName("");
  };

  const addVariantOption = (idx: number) => {
    const opt = variantOptionInput.trim();
    if (!opt) return;
    const updated = [...variants];
    if (!updated[idx].options.includes(opt)) { updated[idx].options.push(opt); setVariants(updated); }
    setVariantOptionInput("");
  };

  const removeVariantOption = (vIdx: number, oIdx: number) => {
    const updated = [...variants]; updated[vIdx].options.splice(oIdx, 1); setVariants(updated);
  };

  const removeVariant = (idx: number) => setVariants(variants.filter((_, i) => i !== idx));

  const addSpec = () => setSpecs([...specs, { key: "", value: "" }]);
  const updateSpec = (idx: number, field: "key" | "value", val: string) => {
    const updated = [...specs]; updated[idx][field] = val; setSpecs(updated);
  };
  const removeSpec = (idx: number) => setSpecs(specs.filter((_, i) => i !== idx));

  const discount = originalPrice && price
    ? Math.round(((parseFloat(originalPrice) - parseFloat(price)) / parseFloat(originalPrice)) * 100) : 0;

  const selectedCategory = categories.find(c => c.slug === category);
  const updateM = useMutation({
    mutationFn: () => vendorProductApi.update(id!, {
      title: name, description,
      categoryId: selectedCategory?.id ?? product?.categoryId,
      basePricePaise: rupeesToPaise(originalPrice || price),
      salePricePaise: originalPrice ? rupeesToPaise(price) : undefined,
      primaryImageUrl: images[0],
    }),
    onSuccess: () => {
      toast({ title: "Product updated!", description: `"${name}" has been saved.` });
      navigate("/vendor/products");
    },
    onError: (e: unknown) => toast({ title: "Update failed", description: (e as Error).message, variant: "destructive" }),
  });
  const handleSubmit = () => {
    if (!name || !price || !category) {
      toast({ title: "Missing fields", description: "Please fill in product name, price, and category.", variant: "destructive" });
      return;
    }
    updateM.mutate();
  };

  if (!product) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <p className="text-lg font-medium">Product not found</p>
          <Button variant="link" onClick={() => navigate("/vendor/products")}>Back to Products</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate("/vendor/products")}><ArrowLeft className="h-4 w-4" /></Button>
        <div className="flex-1">
          <h1 className="font-display text-xl font-bold">Edit Product</h1>
          <p className="text-sm text-muted-foreground">Update details for "{product.name}"</p>
        </div>
        <Badge variant="outline" className="hidden sm:inline-flex">{product.id}</Badge>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-6">
          <Card className="shadow-card">
            <CardHeader className="pb-4"><CardTitle className="text-base">Basic Information</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="name">Product Name *</Label>
                <Input id="name" value={name} onChange={e => setName(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="desc">Description</Label>
                <Textarea id="desc" value={description} onChange={e => setDescription(e.target.value)} rows={4} />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Category *</Label>
                  <Select value={category} onValueChange={v => { setCategory(v); setSubcategory(""); }}>
                    <SelectTrigger><SelectValue placeholder="Select category" /></SelectTrigger>
                    <SelectContent>{categories.map(c => <SelectItem key={c.id} value={c.slug}>{c.icon} {c.name}</SelectItem>)}</SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Subcategory</Label>
                  <Select value={subcategory} onValueChange={setSubcategory} disabled={!selectedCategory}>
                    <SelectTrigger><SelectValue placeholder="Select" /></SelectTrigger>
                    <SelectContent>{selectedCategory?.subcategories.map(s => <SelectItem key={s} value={s}>{s}</SelectItem>)}</SelectContent>
                  </Select>
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="brand">Brand</Label>
                <Input id="brand" value={brand} onChange={e => setBrand(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label>Tags</Label>
                <div className="flex gap-2">
                  <Input value={tagInput} onChange={e => setTagInput(e.target.value)} placeholder="Add a tag..." onKeyDown={e => e.key === "Enter" && (e.preventDefault(), addTag())} />
                  <Button type="button" variant="secondary" size="sm" onClick={addTag}>Add</Button>
                </div>
                {tags.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 mt-2">
                    {tags.map(t => <Badge key={t} variant="secondary" className="gap-1">{t}<X className="h-3 w-3 cursor-pointer" onClick={() => setTags(tags.filter(x => x !== t))} /></Badge>)}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>

          <Card className="shadow-card">
            <CardHeader className="pb-4"><CardTitle className="text-base">Product Images</CardTitle></CardHeader>
            <CardContent>
              <div className="grid grid-cols-3 gap-3">
                {images.map((img, i) => (
                  <div key={i} className="relative group aspect-square rounded-xl overflow-hidden border border-border bg-muted">
                    <img src={img} alt={`Product ${i + 1}`} className="w-full h-full object-cover" />
                    {i === 0 && <span className="absolute top-1.5 left-1.5 bg-primary text-primary-foreground text-[10px] font-semibold px-1.5 py-0.5 rounded">Cover</span>}
                    <button onClick={() => setImages(images.filter((_, j) => j !== i))} className="absolute top-1.5 right-1.5 bg-destructive text-destructive-foreground rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity"><X className="h-3 w-3" /></button>
                  </div>
                ))}
                {images.length < 6 && (
                  <button onClick={handleImageUpload} className="aspect-square rounded-xl border-2 border-dashed border-border hover:border-primary/50 bg-muted/50 flex flex-col items-center justify-center gap-2 text-muted-foreground hover:text-primary transition-colors">
                    <ImagePlus className="h-6 w-6" /><span className="text-xs font-medium">Add Image</span>
                  </button>
                )}
              </div>
            </CardContent>
          </Card>

          <Card className="shadow-card">
            <CardHeader className="pb-4"><CardTitle className="text-base">Variants</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              {variants.map((v, vIdx) => (
                <div key={vIdx} className="rounded-lg border border-border p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2"><GripVertical className="h-4 w-4 text-muted-foreground" /><span className="font-medium text-sm">{v.name}</span></div>
                    <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive" onClick={() => removeVariant(vIdx)}><Trash2 className="h-3.5 w-3.5" /></Button>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {v.options.map((opt, oIdx) => <Badge key={oIdx} variant="outline" className="gap-1">{opt}<X className="h-3 w-3 cursor-pointer" onClick={() => removeVariantOption(vIdx, oIdx)} /></Badge>)}
                  </div>
                  <Input placeholder={`Add ${v.name.toLowerCase()} option...`} className="h-8 text-sm" onKeyDown={e => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      const val = (e.target as HTMLInputElement).value.trim();
                      if (val) {
                        const updated = [...variants];
                        if (!updated[vIdx].options.includes(val)) {
                          updated[vIdx].options.push(val);
                          setVariants(updated);
                        }
                        (e.target as HTMLInputElement).value = "";
                      }
                    }
                  }} />
                </div>
              ))}
              <Separator />
              <div className="flex gap-2">
                <Input value={variantName} onChange={e => setVariantName(e.target.value)} placeholder="Variant name (e.g. Color)" className="h-9" onKeyDown={e => e.key === "Enter" && (e.preventDefault(), addVariant())} />
                <Button type="button" variant="outline" size="sm" onClick={addVariant} className="gap-1 shrink-0"><Plus className="h-3.5 w-3.5" /> Add Variant</Button>
              </div>
            </CardContent>
          </Card>

          <Card className="shadow-card">
            <CardHeader className="pb-4"><CardTitle className="text-base">Specifications</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              {specs.map((s, i) => (
                <div key={i} className="flex gap-2 items-center">
                  <Input value={s.key} onChange={e => updateSpec(i, "key", e.target.value)} placeholder="Key" className="h-9" />
                  <Input value={s.value} onChange={e => updateSpec(i, "value", e.target.value)} placeholder="Value" className="h-9" />
                  <Button variant="ghost" size="icon" className="h-8 w-8 shrink-0 text-destructive" onClick={() => removeSpec(i)}><Trash2 className="h-3.5 w-3.5" /></Button>
                </div>
              ))}
              <Button type="button" variant="outline" size="sm" onClick={addSpec} className="gap-1"><Plus className="h-3.5 w-3.5" /> Add Specification</Button>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card className="shadow-card">
            <CardHeader className="pb-4"><CardTitle className="text-base">Pricing</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2"><Label>Selling Price (₹) *</Label><Input type="number" value={price} onChange={e => setPrice(e.target.value)} /></div>
              <div className="space-y-2"><Label>MRP (₹)</Label><Input type="number" value={originalPrice} onChange={e => setOriginalPrice(e.target.value)} /></div>
              {discount > 0 && <div className="rounded-lg bg-accent/50 p-3 text-center"><span className="text-sm text-muted-foreground">Discount:</span><span className="ml-1.5 text-lg font-bold text-primary">{discount}% off</span></div>}
            </CardContent>
          </Card>

          <Card className="shadow-card">
            <CardHeader className="pb-4"><CardTitle className="text-base">Inventory</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2"><Label>SKU</Label><Input value={sku} onChange={e => setSku(e.target.value)} /></div>
              <div className="space-y-2"><Label>Stock Quantity</Label><Input type="number" value={stockCount} onChange={e => setStockCount(e.target.value)} /></div>
              <div className="space-y-2"><Label>Low Stock Threshold</Label><Input type="number" value={lowStockThreshold} onChange={e => setLowStockThreshold(e.target.value)} /></div>
              <div className="flex items-center justify-between"><Label className="text-sm">Track Inventory</Label><Switch checked={trackInventory} onCheckedChange={setTrackInventory} /></div>
            </CardContent>
          </Card>

          <Card className="shadow-card">
            <CardHeader className="pb-4"><CardTitle className="text-base">Visibility</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              <div className="flex items-center justify-between"><Label className="text-sm">Featured Product</Label><Switch checked={featured} onCheckedChange={setFeatured} /></div>
            </CardContent>
          </Card>

          <div className="space-y-2">
            <Button className="w-full" onClick={handleSubmit}><Save className="h-4 w-4 mr-1.5" /> Save Changes</Button>
            <Button variant="outline" className="w-full" onClick={() => navigate("/vendor/products")}>Cancel</Button>
          </div>
        </div>
      </div>
    </div>
  );
}
