/**
 * Vendor Payout & Settlement domain — backend-ready DTOs.
 *
 * Money is integer ₹. Period is a closed-open range [periodStart, periodEnd).
 * Commission and TDS are computed at settlement-time using the tax rule
 * effective on `periodEnd`.
 */
import type { PayoutState } from "@/lib/fsm";

export interface CommissionRule {
  id: string;
  category?: string;        // null = global default
  vendorId?: string;        // vendor-specific override
  percentage: number;       // e.g. 8.5
  effectiveFrom: string;    // ISO timestamp
  effectiveTo?: string;
}

export interface SettlementLedgerEntry {
  id: string;
  vendorId: string;
  orderId: string;
  childOrderId: string;
  /** Gross merchandise value attributable to the vendor for this child order. */
  gmv: number;
  commission: number;
  shippingReimbursement: number;
  taxCollected: number;
  refundAdjustment: number;
  /** Net = gmv - commission + shippingReimbursement - refundAdjustment. */
  netPayable: number;
  occurredAt: string;
  settlementId?: string;    // populated once swept into a payout
}

export interface Settlement {
  id: string;
  vendorId: string;
  periodStart: string;
  periodEnd: string;
  grossSales: number;
  totalCommission: number;
  totalRefunds: number;
  totalTds: number;
  netPayable: number;
  entries: string[]; // ledger entry ids
  createdAt: string;
}

export interface Payout {
  id: string;
  vendorId: string;
  settlementId: string;
  amount: number;
  status: PayoutState;
  scheduledFor?: string;
  processedAt?: string;
  failureReason?: string;
  bankRef?: string;          // UTR/IMPS/NEFT reference
  idempotencyKey: string;
  createdAt: string;
  updatedAt: string;
}

export interface PayoutSummary {
  vendorId: string;
  available: number;     // net payable not yet swept
  inFlight: number;      // scheduled + processing
  paidLifetime: number;
  lastPayoutAt?: string;
}