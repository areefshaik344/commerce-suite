/**
 * Invoice domain — backend-ready DTOs.
 *
 * Immutable snapshot. Once issued, the invoice MUST NOT change even if
 * the underlying order is modified.
 */
import type { Address } from "@/data/mock-users";
import type { VendorSnapshot, ProductSnapshot } from "./order";

export type InvoiceStatus = "DRAFT" | "ISSUED" | "PAID" | "CANCELLED" | "REFUNDED";

export interface InvoiceLineItem {
  id: string;
  product: ProductSnapshot;
  hsnCode?: string;
  quantity: number;
  unitPrice: number;
  discount: number;
  taxableValue: number;
  cgst: number;
  sgst: number;
  igst: number;
  total: number;
}

export interface InvoiceTaxSummary {
  taxableValue: number;
  cgst: number;
  sgst: number;
  igst: number;
  totalTax: number;
}

export interface Invoice {
  id: string;
  number: string;
  orderId: string;
  vendorOrderId: string;
  vendor: VendorSnapshot;
  customerId: string;
  billingAddress: Address;
  shippingAddress: Address;
  lineItems: InvoiceLineItem[];
  taxSummary: InvoiceTaxSummary;
  shipping: number;
  discount: number;
  grandTotal: number;
  currency: "INR";
  status: InvoiceStatus;
  issuedAt: string;
  pdfUrl: string | null;
  schemaVersion: 1;
}
