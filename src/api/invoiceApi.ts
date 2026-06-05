import { simulateDelay, mockSuccess, ApiError, type ApiResponse } from "./apiClient";
import type { Invoice, InvoiceLineItem, InvoiceTaxSummary } from "@/types/invoice";
import type { OrderRecord, VendorOrder } from "@/types/order";
import { mockOrderRecords } from "@/mocks/mockOrderRecords";

const INVOICES: Record<string, Invoice> = {};
const seq = (p: string) => `${p}-${Date.now().toString(36)}-${Math.floor(Math.random() * 1e4).toString(36)}`;
const num = () => `INV-${new Date().getFullYear()}-${Math.floor(Math.random() * 1e6).toString().padStart(6, "0")}`;

function buildInvoice(order: OrderRecord, vo: VendorOrder): Invoice {
  const items = order.items.filter(i => vo.itemIds.includes(i.id));
  const GST = 0.18;
  const lineItems: InvoiceLineItem[] = items.map(it => {
    const taxable = it.pricing.subtotal - it.pricing.discount;
    const cgst = Math.round((taxable * GST) / 2);
    const sgst = Math.round((taxable * GST) / 2);
    return {
      id: seq("IL"),
      product: it.product,
      quantity: it.pricing.quantity,
      unitPrice: it.pricing.unitPrice,
      discount: it.pricing.discount,
      taxableValue: taxable,
      cgst, sgst, igst: 0,
      total: taxable + cgst + sgst,
    };
  });
  const taxSummary: InvoiceTaxSummary = lineItems.reduce<InvoiceTaxSummary>((acc, l) => ({
    taxableValue: acc.taxableValue + l.taxableValue,
    cgst: acc.cgst + l.cgst, sgst: acc.sgst + l.sgst, igst: acc.igst + l.igst,
    totalTax: acc.totalTax + l.cgst + l.sgst + l.igst,
  }), { taxableValue: 0, cgst: 0, sgst: 0, igst: 0, totalTax: 0 });
  return {
    id: seq("INV"),
    number: num(),
    orderId: order.id,
    vendorOrderId: vo.id,
    vendor: vo.vendor,
    customerId: order.customerId,
    billingAddress: order.shippingAddress,
    shippingAddress: order.shippingAddress,
    lineItems,
    taxSummary,
    shipping: vo.shipping,
    discount: vo.discount,
    grandTotal: taxSummary.taxableValue + taxSummary.totalTax + vo.shipping,
    currency: "INR",
    status: order.payment.status === "CAPTURED" ? "PAID" : "ISSUED",
    issuedAt: order.placedAt,
    pdfUrl: null,
    schemaVersion: 1,
  };
}

export const invoiceApi = {
  async listForOrder(orderId: string): Promise<ApiResponse<Invoice[]>> {
    await simulateDelay(180);
    const order = mockOrderRecords.find(o => o.id === orderId);
    if (!order) throw new ApiError("Order not found", 404, "ORDER_NOT_FOUND");
    const cached = Object.values(INVOICES).filter(i => i.orderId === orderId);
    if (cached.length > 0) return mockSuccess(cached);
    const fresh = order.vendorOrders.map(vo => buildInvoice(order, vo));
    fresh.forEach(inv => { INVOICES[inv.id] = inv; });
    return mockSuccess(fresh);
  },

  async getById(id: string): Promise<ApiResponse<Invoice>> {
    await simulateDelay(120);
    const inv = INVOICES[id];
    if (!inv) throw new ApiError("Invoice not found", 404, "INVOICE_NOT_FOUND");
    return mockSuccess(inv);
  },

  async download(id: string): Promise<ApiResponse<{ url: string }>> {
    await simulateDelay(150);
    if (!INVOICES[id]) throw new ApiError("Invoice not found", 404, "INVOICE_NOT_FOUND");
    // Backend will return signed URL; mock returns placeholder.
    return mockSuccess({ url: `/invoices/${id}.pdf` });
  },
};
