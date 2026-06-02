/**
 * Category → attribute schema.
 *
 * Drives:
 *  - dynamic filter UI on category/listing pages
 *  - dynamic spec rendering on PDP
 *  - vendor product form field generation
 *
 * Keep this config-driven. Never hardcode attribute keys inside components.
 * Backend will eventually serve this from an `/attributes/schema?category=...`
 * endpoint — keep the shape stable.
 */

export type AttributeInputType = "select" | "multiselect" | "text" | "number" | "range" | "boolean";

export interface AttributeDef {
  key: string;
  label: string;
  type: AttributeInputType;
  /** When true, surfaced as a filter facet on the listing page. */
  filterable: boolean;
  /** When true, surfaced in the PDP specs panel. */
  visibleOnPdp: boolean;
  /** When true, may drive a variant axis (size, color, storage, ...). */
  variantAxis?: boolean;
  unit?: string;
  options?: string[];
}

/**
 * Category slug → attribute definitions. Slugs match `mockCategories[].slug`.
 * `*` is the fallback used when a category has no specific schema.
 */
export const CATEGORY_ATTRIBUTES: Record<string, AttributeDef[]> = {
  electronics: [
    { key: "RAM", label: "RAM", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true, options: ["4GB", "8GB", "12GB", "16GB", "24GB", "32GB", "64GB"] },
    { key: "Storage", label: "Storage", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true, options: ["64GB", "128GB", "256GB", "512GB", "1TB", "2TB"] },
    { key: "Processor", label: "Processor", type: "text", filterable: true, visibleOnPdp: true },
    { key: "Display", label: "Display", type: "text", filterable: false, visibleOnPdp: true },
    { key: "Battery", label: "Battery", type: "text", filterable: false, visibleOnPdp: true },
    { key: "Color", label: "Color", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true },
  ],
  fashion: [
    { key: "Size", label: "Size", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true, options: ["XS", "S", "M", "L", "XL", "XXL", "28", "30", "32", "34", "36", "38", "40"] },
    { key: "Color", label: "Color", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true },
    { key: "Material", label: "Material", type: "text", filterable: true, visibleOnPdp: true },
    { key: "Fit", label: "Fit", type: "select", filterable: true, visibleOnPdp: true, options: ["Slim", "Regular", "Oversized", "Relaxed"] },
  ],
  "home-living": [
    { key: "Material", label: "Material", type: "text", filterable: true, visibleOnPdp: true },
    { key: "Dimensions", label: "Dimensions", type: "text", filterable: false, visibleOnPdp: true },
    { key: "Color", label: "Color", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true },
  ],
  beauty: [
    { key: "Volume", label: "Volume", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true, unit: "ml" },
    { key: "Skin Type", label: "Skin Type", type: "multiselect", filterable: true, visibleOnPdp: true, options: ["Oily", "Dry", "Combination", "Sensitive", "Normal"] },
    { key: "Shade", label: "Shade", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true },
  ],
  sports: [
    { key: "Size", label: "Size", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true, options: ["XS", "S", "M", "L", "XL"] },
    { key: "Material", label: "Material", type: "text", filterable: true, visibleOnPdp: true },
    { key: "Color", label: "Color", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true },
  ],
  books: [
    { key: "Language", label: "Language", type: "select", filterable: true, visibleOnPdp: true, options: ["English", "Hindi", "Tamil", "Bengali", "Marathi"] },
    { key: "Format", label: "Format", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true, options: ["Paperback", "Hardcover", "eBook", "Audiobook"] },
    { key: "Pages", label: "Pages", type: "number", filterable: false, visibleOnPdp: true },
  ],
  groceries: [
    { key: "Weight", label: "Weight", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true, unit: "g" },
    { key: "Organic", label: "Organic", type: "boolean", filterable: true, visibleOnPdp: true },
  ],
  toys: [
    { key: "Age Group", label: "Age Group", type: "select", filterable: true, visibleOnPdp: true, options: ["0-2", "3-5", "6-8", "9-12", "13+"] },
    { key: "Material", label: "Material", type: "text", filterable: true, visibleOnPdp: true },
  ],
  "*": [
    { key: "Color", label: "Color", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true },
    { key: "Size", label: "Size", type: "select", filterable: true, visibleOnPdp: true, variantAxis: true },
  ],
};

export function getAttributeSchema(categorySlug: string | undefined | null): AttributeDef[] {
  if (!categorySlug) return CATEGORY_ATTRIBUTES["*"];
  return CATEGORY_ATTRIBUTES[categorySlug] ?? CATEGORY_ATTRIBUTES["*"];
}

export function getFilterableAttributes(categorySlug: string | undefined | null): AttributeDef[] {
  return getAttributeSchema(categorySlug).filter((a) => a.filterable);
}

export function getVariantAxes(categorySlug: string | undefined | null): AttributeDef[] {
  return getAttributeSchema(categorySlug).filter((a) => a.variantAxis);
}

export function getPdpSpecAttributes(categorySlug: string | undefined | null): AttributeDef[] {
  return getAttributeSchema(categorySlug).filter((a) => a.visibleOnPdp);
}