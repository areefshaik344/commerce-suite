import { mockSuccess, simulateDelay, ApiError, type ApiResponse } from "./apiClient";
import { mockProducts } from "@/mocks";
import { mockVendors } from "@/mocks";
import { mockOrders } from "@/mocks";
import type { Product } from "@/data/mock-products";
import type { Vendor } from "@/data/mock-users";
import type { Order } from "@/data/mock-orders";
import { httpClient, USE_REAL_API } from "./httpClient";

/* -------------------------------------------------------------------------- */
/* Backend DTOs (mirrors com.commercesuite.vendor.dto.*)                      */
/* -------------------------------------------------------------------------- */

export type BackendVendorStatus =
  | "PENDING" | "APPROVED" | "ACTIVE" | "SUSPENDED" | "REJECTED" | "DEACTIVATED";
export type BackendApplicationStatus =
  | "SUBMITTED" | "UNDER_REVIEW" | "APPROVED" | "REJECTED" | "WITHDRAWN";
export type BackendVerificationStatus =
  | "PENDING" | "APPROVED" | "REJECTED";
export type BackendDocumentType =
  | "PAN" | "GSTIN" | "AADHAAR" | "BUSINESS_LICENSE" | "BANK_STATEMENT"
  | "CANCELLED_CHEQUE" | "OTHER";

export interface VendorDto {
  id: string; userId: string; legalName: string; displayName: string;
  status: BackendVendorStatus; statusReason: string | null;
  approvedAt: string | null; rejectedAt: string | null;
  suspendedAt: string | null; deactivatedAt: string | null;
  createdAt: string; updatedAt: string;
}

export interface VendorProfileDto {
  id: string; vendorId: string; storeName: string; storeSlug: string;
  description: string | null; logoUrl: string | null; bannerUrl: string | null;
  supportEmail: string | null; supportPhone: string | null;
  websiteUrl: string | null; returnPolicy: string | null;
}

export interface VendorVerificationDto {
  vendorId: string; kycStatus: BackendVerificationStatus;
  bankStatus: BackendVerificationStatus; profileComplete: boolean;
}

export interface VendorDocumentDto {
  id: string; vendorId: string; documentType: BackendDocumentType;
  documentNumber: string | null; fileUrl: string | null; fileMime: string | null;
  fileSizeBytes: number | null; verificationStatus: BackendVerificationStatus;
  reviewNotes: string | null; reviewedAt: string | null; uploadedAt: string;
}

export interface VendorBankAccountDto {
  id: string; vendorId: string; accountHolderName: string;
  accountNumberMasked: string; ifscCode: string; bankName: string;
  branchName: string | null; verificationStatus: BackendVerificationStatus;
  verifiedAt: string | null; primary: boolean;
}

export interface VendorApplicationDto {
  id: string; userId: string; vendorId: string | null;
  status: BackendApplicationStatus;
  businessName: string; businessType: string;
  gstin: string | null; pan: string | null;
  contactEmail: string; contactPhone: string; registeredAddress: string;
  submittedAt: string | null; reviewedAt: string | null;
  reviewNotes: string | null; createdAt: string;
}

export interface ApplyVendorRequest {
  legalName: string; displayName: string;
  businessName: string; businessType: string;
  gstin?: string | null; pan?: string | null;
  contactEmail: string; contactPhone: string; registeredAddress: string;
}

export interface UpdateVendorProfileRequest {
  storeName: string; description?: string;
  logoUrl?: string; bannerUrl?: string;
  supportEmail?: string; supportPhone?: string;
  websiteUrl?: string; returnPolicy?: string;
}

export interface UpsertBankAccountRequest {
  accountHolderName: string; accountNumber: string;
  ifscCode: string; bankName: string; branchName?: string;
}

export interface UpsertDocumentRequest {
  documentType: BackendDocumentType;
  documentNumber?: string;
  fileUrl?: string; fileMime?: string; fileSizeBytes?: number;
}

/* -------------------------------------------------------------------------- */
/* Real-backend surface (BE: VendorController @ /api/v1/vendors)              */
/* -------------------------------------------------------------------------- */

export const vendorApi = {
  /* ---- Application & profile (backend-wired) ---- */

  async apply(req: ApplyVendorRequest): Promise<ApiResponse<VendorApplicationDto>> {
    if (USE_REAL_API) return httpClient.post<VendorApplicationDto>("/vendors/apply", req);
    await simulateDelay(300);
    return mockSuccess({
      id: `app-${Date.now()}`, userId: "mock", vendorId: null, status: "SUBMITTED",
      businessName: req.businessName, businessType: req.businessType,
      gstin: req.gstin ?? null, pan: req.pan ?? null,
      contactEmail: req.contactEmail, contactPhone: req.contactPhone,
      registeredAddress: req.registeredAddress,
      submittedAt: new Date().toISOString(), reviewedAt: null, reviewNotes: null,
      createdAt: new Date().toISOString(),
    }, "Application submitted");
  },

  async myApplications(): Promise<ApiResponse<VendorApplicationDto[]>> {
    if (USE_REAL_API) return httpClient.get<VendorApplicationDto[]>("/vendors/me/applications");
    await simulateDelay(200);
    return mockSuccess([]);
  },

  async me(): Promise<ApiResponse<VendorDto>> {
    if (USE_REAL_API) return httpClient.get<VendorDto>("/vendors/me");
    throw new ApiError("Vendor not provisioned in mock mode", 404, "VENDOR_NOT_FOUND");
  },

  async myProfile(): Promise<ApiResponse<VendorProfileDto>> {
    if (USE_REAL_API) return httpClient.get<VendorProfileDto>("/vendors/me/profile");
    throw new ApiError("Vendor profile unavailable in mock mode", 404, "VENDOR_PROFILE_NOT_FOUND");
  },

  async updateProfileV2(req: UpdateVendorProfileRequest): Promise<ApiResponse<VendorProfileDto>> {
    if (USE_REAL_API) return httpClient.put<VendorProfileDto>("/vendors/me", req);
    await simulateDelay(300);
    throw new ApiError("Vendor profile updates require real backend", 501, "NOT_IMPLEMENTED");
  },

  async myVerification(): Promise<ApiResponse<VendorVerificationDto>> {
    if (USE_REAL_API) return httpClient.get<VendorVerificationDto>("/vendors/me/verification");
    await simulateDelay(150);
    return mockSuccess({
      vendorId: "mock", kycStatus: "PENDING", bankStatus: "PENDING", profileComplete: false,
    });
  },

  async uploadDocument(req: UpsertDocumentRequest): Promise<ApiResponse<VendorDocumentDto>> {
    if (USE_REAL_API) return httpClient.post<VendorDocumentDto>("/vendors/me/documents", req);
    await simulateDelay(250);
    return mockSuccess({
      id: `doc-${Date.now()}`, vendorId: "mock", documentType: req.documentType,
      documentNumber: req.documentNumber ?? null,
      fileUrl: req.fileUrl ?? null, fileMime: req.fileMime ?? null,
      fileSizeBytes: req.fileSizeBytes ?? null,
      verificationStatus: "PENDING", reviewNotes: null, reviewedAt: null,
      uploadedAt: new Date().toISOString(),
    }, "Document submitted");
  },

  async listDocuments(): Promise<ApiResponse<VendorDocumentDto[]>> {
    if (USE_REAL_API) return httpClient.get<VendorDocumentDto[]>("/vendors/me/documents");
    await simulateDelay(150);
    return mockSuccess([]);
  },

  async upsertBank(req: UpsertBankAccountRequest): Promise<ApiResponse<VendorBankAccountDto>> {
    if (USE_REAL_API) return httpClient.post<VendorBankAccountDto>("/vendors/me/bank-account", req);
    await simulateDelay(250);
    return mockSuccess({
      id: `bank-${Date.now()}`, vendorId: "mock",
      accountHolderName: req.accountHolderName,
      accountNumberMasked: "****" + req.accountNumber.slice(-4),
      ifscCode: req.ifscCode, bankName: req.bankName,
      branchName: req.branchName ?? null,
      verificationStatus: "PENDING", verifiedAt: null, primary: true,
    }, "Bank account saved");
  },

  async listBankAccounts(): Promise<ApiResponse<VendorBankAccountDto[]>> {
    if (USE_REAL_API) return httpClient.get<VendorBankAccountDto[]>("/vendors/me/bank-accounts");
    await simulateDelay(150);
    return mockSuccess([]);
  },

  /* ---- Legacy mock surface (used by existing vendor pages) ---- */

  async getVendorProfile(vendorId: string): Promise<ApiResponse<Vendor | null>> {
    await simulateDelay(200);
    return mockSuccess(mockVendors.find(v => v.id === vendorId) || null);
  },

  async getVendorProducts(vendorId: string): Promise<ApiResponse<Product[]>> {
    await simulateDelay(200);
    return mockSuccess(mockProducts.filter(p => p.vendorId === vendorId));
  },

  async getVendorOrders(vendorId: string): Promise<ApiResponse<Order[]>> {
    await simulateDelay(300);
    return mockSuccess(mockOrders.filter(o => o.vendorId === vendorId));
  },

  async getVendorBySlug(slug: string): Promise<ApiResponse<Vendor | null>> {
    await simulateDelay(200);
    const vendor = mockVendors.find(v => v.storeName.toLowerCase().replace(/\s+/g, "-") === slug);
    return mockSuccess(vendor || null);
  },

  async updateVendorProfile(vendorId: string, data: Partial<Vendor>): Promise<ApiResponse<Vendor>> {
    await simulateDelay(400);
    const idx = mockVendors.findIndex(v => v.id === vendorId);
    if (idx === -1) throw new Error("Vendor not found");
    Object.assign(mockVendors[idx], data);
    return mockSuccess(mockVendors[idx], "Profile updated");
  },

  async createProduct(product: Omit<Product, "id" | "slug">): Promise<ApiResponse<Product>> {
    await simulateDelay(600);
    const newProduct: Product = {
      ...product,
      id: `prod-${Date.now()}`,
      slug: product.name.toLowerCase().replace(/\s+/g, "-").replace(/[^a-z0-9-]/g, ""),
    } as Product;
    mockProducts.push(newProduct);
    return mockSuccess(newProduct, "Product created");
  },

  async updateProduct(productId: string, data: Partial<Product>): Promise<ApiResponse<Product>> {
    await simulateDelay(400);
    const idx = mockProducts.findIndex(p => p.id === productId);
    if (idx === -1) throw new Error("Product not found");
    Object.assign(mockProducts[idx], data);
    return mockSuccess(mockProducts[idx], "Product updated");
  },

  async deleteProduct(productId: string): Promise<ApiResponse<{ deleted: boolean }>> {
    await simulateDelay(300);
    const idx = mockProducts.findIndex(p => p.id === productId);
    if (idx !== -1) mockProducts.splice(idx, 1);
    return mockSuccess({ deleted: true }, "Product deleted");
  },

  async getVendorAnalytics(vendorId: string): Promise<ApiResponse<{
    revenue: number;
    orders: number;
    products: number;
    avgRating: number;
    monthlySales: { month: string; revenue: number; orders: number }[];
  }>> {
    await simulateDelay(300);
    const products = mockProducts.filter(p => p.vendorId === vendorId);
    const orders = mockOrders.filter(o => o.vendorId === vendorId);
    return mockSuccess({
      revenue: orders.reduce((s, o) => s + o.total, 0),
      orders: orders.length,
      products: products.length,
      avgRating: products.length ? products.reduce((s, p) => s + p.rating, 0) / products.length : 0,
      monthlySales: [
        { month: "Jan", revenue: 2800000, orders: 120 },
        { month: "Feb", revenue: 3200000, orders: 145 },
        { month: "Mar", revenue: 2900000, orders: 132 },
      ],
    });
  },

  async getAllVendors(): Promise<ApiResponse<Vendor[]>> {
    await simulateDelay(200);
    return mockSuccess(mockVendors);
  },
};
