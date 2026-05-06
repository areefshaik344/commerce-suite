import { create } from "zustand";
import { persist } from "zustand/middleware";

export type OnboardingStageId =
  | "account"
  | "business"
  | "tax"
  | "pickup"
  | "bank"
  | "signature"
  | "catalog"
  | "golive";

export interface OnboardingStageMeta {
  id: OnboardingStageId;
  title: string;
  shortTitle: string;
  description: string;
}

export const ONBOARDING_STAGES: OnboardingStageMeta[] = [
  { id: "account", title: "Account Verification", shortTitle: "Account", description: "Verify your contact details" },
  { id: "business", title: "Business Information", shortTitle: "Business", description: "Tell us about your store" },
  { id: "tax", title: "Tax Details (GST & PAN)", shortTitle: "Tax", description: "Add GSTIN and PAN for compliance" },
  { id: "pickup", title: "Pickup Address", shortTitle: "Pickup", description: "Where we collect your shipments" },
  { id: "bank", title: "Bank Account", shortTitle: "Bank", description: "Add your settlement account" },
  { id: "signature", title: "Signature & Agreement", shortTitle: "Signature", description: "Upload signature & accept seller agreement" },
  { id: "catalog", title: "Catalog Setup", shortTitle: "Catalog", description: "List your first product to go live" },
  { id: "golive", title: "Go Live", shortTitle: "Go Live", description: "Submit for final review and start selling" },
];

export type StageStatus = "not_started" | "in_progress" | "submitted" | "verified" | "rejected";

export interface AccountInfo {
  emailVerified: boolean;
  phoneVerified: boolean;
}
export interface BusinessInfo {
  businessName: string;
  legalName: string;
  businessType: string;
  category: string;
  description: string;
  yearEstablished: string;
  website: string;
}
export interface TaxInfo {
  hasGst: "yes" | "no" | "";
  gstNumber: string;
  panNumber: string;
  gstDocUploaded: boolean;
  panDocUploaded: boolean;
}
export interface PickupAddress {
  contactName: string;
  phone: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  pincode: string;
  landmark: string;
}
export interface BankInfo {
  accountHolder: string;
  accountNumber: string;
  confirmAccountNumber: string;
  ifscCode: string;
  bankName: string;
  branch: string;
  accountType: "savings" | "current" | "";
  chequeUploaded: boolean;
  pennyDropVerified: boolean;
}
export interface SignatureInfo {
  signatureUploaded: boolean;
  agreementAccepted: boolean;
  authorizedSignatory: string;
  designation: string;
}
export interface CatalogInfo {
  productsAdded: number;
  primaryWarehouseConfirmed: boolean;
  shippingProvider: "self" | "platform" | "";
}

export interface TimelineEvent {
  id: string;
  stage: OnboardingStageId | "system";
  status: "submitted" | "verified" | "rejected" | "info";
  message: string;
  at: string;
}

export interface OnboardingState {
  startedAt: string;
  currentStage: OnboardingStageId;
  stageStatus: Record<OnboardingStageId, StageStatus>;
  rejectionNotes: Partial<Record<OnboardingStageId, string>>;
  account: AccountInfo;
  business: BusinessInfo;
  tax: TaxInfo;
  pickup: PickupAddress;
  bank: BankInfo;
  signature: SignatureInfo;
  catalog: CatalogInfo;
  timeline: TimelineEvent[];
  finalStatus: "draft" | "under_review" | "approved" | "rejected";
}

const emptyState: OnboardingState = {
  startedAt: new Date().toISOString(),
  currentStage: "account",
  stageStatus: {
    account: "not_started",
    business: "not_started",
    tax: "not_started",
    pickup: "not_started",
    bank: "not_started",
    signature: "not_started",
    catalog: "not_started",
    golive: "not_started",
  },
  rejectionNotes: {},
  account: { emailVerified: false, phoneVerified: false },
  business: { businessName: "", legalName: "", businessType: "", category: "", description: "", yearEstablished: "", website: "" },
  tax: { hasGst: "", gstNumber: "", panNumber: "", gstDocUploaded: false, panDocUploaded: false },
  pickup: { contactName: "", phone: "", addressLine1: "", addressLine2: "", city: "", state: "", pincode: "", landmark: "" },
  bank: { accountHolder: "", accountNumber: "", confirmAccountNumber: "", ifscCode: "", bankName: "", branch: "", accountType: "", chequeUploaded: false, pennyDropVerified: false },
  signature: { signatureUploaded: false, agreementAccepted: false, authorizedSignatory: "", designation: "" },
  catalog: { productsAdded: 0, primaryWarehouseConfirmed: false, shippingProvider: "" },
  timeline: [],
  finalStatus: "draft",
};

interface OnboardingStore extends OnboardingState {
  setStage: (stage: OnboardingStageId) => void;
  updateAccount: (data: Partial<AccountInfo>) => void;
  updateBusiness: (data: Partial<BusinessInfo>) => void;
  updateTax: (data: Partial<TaxInfo>) => void;
  updatePickup: (data: Partial<PickupAddress>) => void;
  updateBank: (data: Partial<BankInfo>) => void;
  updateSignature: (data: Partial<SignatureInfo>) => void;
  updateCatalog: (data: Partial<CatalogInfo>) => void;
  submitStage: (stage: OnboardingStageId) => void;
  verifyStage: (stage: OnboardingStageId) => void;
  rejectStage: (stage: OnboardingStageId, note: string) => void;
  submitForReview: () => void;
  approveOnboarding: () => void;
  rejectOnboarding: (note: string) => void;
  reset: () => void;
  completionPercent: () => number;
}

function pushEvent(state: OnboardingState, ev: Omit<TimelineEvent, "id" | "at">): TimelineEvent[] {
  return [
    { ...ev, id: `ev-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`, at: new Date().toISOString() },
    ...state.timeline,
  ];
}

export const useVendorOnboardingStore = create<OnboardingStore>()(
  persist(
    (set, get) => ({
      ...emptyState,

      setStage: (stage) => set({ currentStage: stage }),

      updateAccount: (data) => set((s) => ({ account: { ...s.account, ...data }, stageStatus: { ...s.stageStatus, account: "in_progress" } })),
      updateBusiness: (data) => set((s) => ({ business: { ...s.business, ...data }, stageStatus: { ...s.stageStatus, business: "in_progress" } })),
      updateTax: (data) => set((s) => ({ tax: { ...s.tax, ...data }, stageStatus: { ...s.stageStatus, tax: "in_progress" } })),
      updatePickup: (data) => set((s) => ({ pickup: { ...s.pickup, ...data }, stageStatus: { ...s.stageStatus, pickup: "in_progress" } })),
      updateBank: (data) => set((s) => ({ bank: { ...s.bank, ...data }, stageStatus: { ...s.stageStatus, bank: "in_progress" } })),
      updateSignature: (data) => set((s) => ({ signature: { ...s.signature, ...data }, stageStatus: { ...s.stageStatus, signature: "in_progress" } })),
      updateCatalog: (data) => set((s) => ({ catalog: { ...s.catalog, ...data }, stageStatus: { ...s.stageStatus, catalog: "in_progress" } })),

      submitStage: (stage) => set((s) => {
        const meta = ONBOARDING_STAGES.find((x) => x.id === stage);
        return {
          stageStatus: { ...s.stageStatus, [stage]: "submitted" },
          timeline: pushEvent(s, { stage, status: "submitted", message: `${meta?.title} submitted for verification` }),
        };
      }),
      verifyStage: (stage) => set((s) => {
        const meta = ONBOARDING_STAGES.find((x) => x.id === stage);
        const { [stage]: _omit, ...rest } = s.rejectionNotes;
        return {
          stageStatus: { ...s.stageStatus, [stage]: "verified" },
          rejectionNotes: rest,
          timeline: pushEvent(s, { stage, status: "verified", message: `${meta?.title} verified by admin` }),
        };
      }),
      rejectStage: (stage, note) => set((s) => {
        const meta = ONBOARDING_STAGES.find((x) => x.id === stage);
        return {
          stageStatus: { ...s.stageStatus, [stage]: "rejected" },
          rejectionNotes: { ...s.rejectionNotes, [stage]: note },
          timeline: pushEvent(s, { stage, status: "rejected", message: `${meta?.title} rejected: ${note}` }),
        };
      }),

      submitForReview: () => set((s) => ({
        finalStatus: "under_review",
        stageStatus: { ...s.stageStatus, golive: "submitted" },
        timeline: pushEvent(s, { stage: "golive", status: "submitted", message: "Application submitted for final review" }),
      })),
      approveOnboarding: () => set((s) => ({
        finalStatus: "approved",
        stageStatus: { ...s.stageStatus, golive: "verified" },
        timeline: pushEvent(s, { stage: "system", status: "verified", message: "🎉 Seller account approved. You are now live!" }),
      })),
      rejectOnboarding: (note) => set((s) => ({
        finalStatus: "rejected",
        stageStatus: { ...s.stageStatus, golive: "rejected" },
        timeline: pushEvent(s, { stage: "system", status: "rejected", message: `Application rejected: ${note}` }),
      })),

      reset: () => set({ ...emptyState, startedAt: new Date().toISOString() }),

      completionPercent: () => {
        const s = get();
        const done = ONBOARDING_STAGES.filter((x) => ["submitted", "verified"].includes(s.stageStatus[x.id])).length;
        return Math.round((done / ONBOARDING_STAGES.length) * 100);
      },
    }),
    {
      name: "markethub-vendor-onboarding",
    }
  )
);