import { User, Vendor, Address, NotificationPreferences, VendorBusinessProfile, DeviceSession } from "@/data/mock-users";

const defaultPrefs: NotificationPreferences = {
  emailOrders: true,
  emailPromotions: false,
  emailNewsletter: false,
  smsOrders: true,
  smsPromotions: false,
  pushOrders: true,
  pushPromotions: false,
  whatsappOrders: false,
  language: "en",
  theme: "system",
  privacyShowProfile: true,
};

const priyaVendorProfile: VendorBusinessProfile = {
  vendorId: "v-1",
  businessName: "AppleZone Official",
  legalName: "AppleZone Retail Pvt Ltd",
  storeSlug: "applezone-official",
  supportEmail: "support@applezone.in",
  supportPhone: "+91 80000 11122",
  description: "Authorized Apple reseller across India.",
  logo: "🍎",
  category: "Electronics",
  gstin: "29ABCDE1234F1Z5",
  pan: "ABCDE1234F",
  businessAddress: {
    id: "ba-1", label: "Warehouse", name: "Priya Patel", phone: "+91 87654 32109",
    line1: "Plot 21, Industrial Area Phase II", line2: "Hosur Road",
    city: "Bangalore", state: "Karnataka", pincode: "560100", isDefault: true, type: "WORK",
  },
  bank: {
    accountHolder: "AppleZone Retail Pvt Ltd",
    accountNumberMasked: "••••••6721",
    ifsc: "HDFC0001234", bankName: "HDFC Bank", branch: "Koramangala",
    accountType: "current", verified: true,
  },
  verificationStatus: "verified",
  verifiedAt: "2023-03-12T10:00:00.000Z",
};

export const mockUsers: User[] = [
  { id: "u-1", name: "Rahul Sharma", email: "rahul@example.com", avatar: "", role: "customer", phone: "+91 98765 43210", joinedDate: "2023-06-15", isVendor: false, vendorStatus: "none", emailVerified: true, phoneVerified: true, gender: "male", dob: "1995-04-12", bio: "Tech enthusiast and weekend traveler.", preferences: { ...defaultPrefs }, status: "active", passwordChangedAt: "2024-11-01T10:00:00.000Z", addresses: [
    { id: "a-1", label: "Home", name: "Rahul Sharma", phone: "+91 98765 43210", line1: "42, Marine Drive Apartments", line2: "Near Gateway of India", city: "Mumbai", state: "Maharashtra", pincode: "400001", isDefault: true },
    { id: "a-2", label: "Office", name: "Rahul Sharma", phone: "+91 98765 43211", line1: "Tech Park, Tower B, 5th Floor", line2: "Whitefield", city: "Bangalore", state: "Karnataka", pincode: "560066", isDefault: false },
  ]},
  { id: "u-2", name: "Priya Patel", email: "priya@vendor.com", avatar: "", role: "vendor", phone: "+91 87654 32109", joinedDate: "2023-03-10", isVendor: true, vendorStatus: "active", emailVerified: true, phoneVerified: true, gender: "female", dob: "1990-08-22", bio: "Founder of AppleZone Official.", preferences: { ...defaultPrefs }, vendorProfile: priyaVendorProfile, status: "active", passwordChangedAt: "2024-09-15T10:00:00.000Z" },
  { id: "u-3", name: "Admin User", email: "admin@marketplace.com", avatar: "", role: "admin", phone: "+91 99999 00000", joinedDate: "2022-01-01", emailVerified: true, phoneVerified: true, preferences: { ...defaultPrefs }, status: "active", passwordChangedAt: "2024-12-01T10:00:00.000Z" },
  { id: "u-4", name: "Anita Singh", email: "anita@example.com", avatar: "", role: "customer", phone: "+91 76543 21098", joinedDate: "2023-09-22", isVendor: false, vendorStatus: "pending", preferences: { ...defaultPrefs }, status: "active" },
  { id: "u-5", name: "Vikram Joshi", email: "vikram@example.com", avatar: "", role: "customer", phone: "+91 65432 10987", joinedDate: "2024-01-05", isVendor: false, vendorStatus: "none", preferences: { ...defaultPrefs }, status: "active" },
  { id: "u-6", name: "Amit Kumar", email: "amit@example.com", avatar: "", role: "customer", phone: "+91 54321 09876", joinedDate: "2024-02-14", isVendor: false, vendorStatus: "none" },
  { id: "u-7", name: "Sneha Reddy", email: "sneha@example.com", avatar: "", role: "customer", phone: "+91 43210 98765", joinedDate: "2024-03-20", isVendor: false, vendorStatus: "none" },
  { id: "u-8", name: "Karan Mehta", email: "karan@vendor.com", avatar: "", role: "vendor", phone: "+91 32109 87654", joinedDate: "2023-08-15", isVendor: true, vendorStatus: "active" },
  { id: "u-9", name: "Deepika Nair", email: "deepika@example.com", avatar: "", role: "customer", phone: "+91 21098 76543", joinedDate: "2024-04-10", isVendor: false, vendorStatus: "none" },
  { id: "u-10", name: "Rajesh Gupta", email: "rajesh@example.com", avatar: "", role: "customer", phone: "+91 10987 65432", joinedDate: "2024-05-25", isVendor: false, vendorStatus: "none" },
];

/** In-memory mock device sessions store, keyed by userId. */
export const mockSessions: Record<string, DeviceSession[]> = {
  "u-1": [
    { id: "s-1", device: "MacBook Pro", browser: "Chrome 124", os: "macOS 14", ip: "103.21.5.10", location: "Mumbai, IN", lastActiveAt: new Date().toISOString(), createdAt: "2025-04-10T10:00:00.000Z", current: true },
    { id: "s-2", device: "iPhone 15", browser: "Safari", os: "iOS 17", ip: "103.21.5.44", location: "Mumbai, IN", lastActiveAt: new Date(Date.now() - 86400000).toISOString(), createdAt: "2025-03-22T10:00:00.000Z", current: false },
    { id: "s-3", device: "Windows PC", browser: "Edge 122", os: "Windows 11", ip: "49.36.18.92", location: "Pune, IN", lastActiveAt: new Date(Date.now() - 7 * 86400000).toISOString(), createdAt: "2025-02-14T10:00:00.000Z", current: false },
  ],
  "u-2": [
    { id: "s-4", device: "MacBook Air", browser: "Chrome 124", os: "macOS 14", ip: "117.96.10.5", location: "Bangalore, IN", lastActiveAt: new Date().toISOString(), createdAt: "2025-04-15T10:00:00.000Z", current: true },
  ],
  "u-3": [
    { id: "s-5", device: "MacBook Pro", browser: "Chrome 124", os: "macOS 14", ip: "10.0.0.1", location: "Delhi, IN", lastActiveAt: new Date().toISOString(), createdAt: "2025-04-20T10:00:00.000Z", current: true },
  ],
};

export const mockVendors: Vendor[] = [
  { id: "v-1", userId: "u-2", storeName: "AppleZone Official", logo: "🍎", description: "Authorized Apple reseller", rating: 4.8, totalProducts: 45, totalOrders: 12500, totalRevenue: 89500000, joinedDate: "2023-03-10", status: "active", category: "Electronics" },
  { id: "v-2", userId: "u-6", storeName: "Samsung Flagship Store", logo: "📱", description: "Official Samsung store", rating: 4.6, totalProducts: 78, totalOrders: 9800, totalRevenue: 67200000, joinedDate: "2023-02-15", status: "active", category: "Electronics" },
  { id: "v-3", userId: "u-7", storeName: "SoundWorld", logo: "🎧", description: "Premium audio equipment", rating: 4.5, totalProducts: 120, totalOrders: 15600, totalRevenue: 34500000, joinedDate: "2023-01-20", status: "active", category: "Electronics" },
  { id: "v-4", userId: "u-8", storeName: "SneakerHub", logo: "👟", description: "Authentic sneakers & sportswear", rating: 4.3, totalProducts: 230, totalOrders: 8900, totalRevenue: 12300000, joinedDate: "2023-05-11", status: "active", category: "Fashion" },
  { id: "v-5", userId: "u-9", storeName: "DenimWorld", logo: "👖", description: "Premium denim collection", rating: 4.4, totalProducts: 89, totalOrders: 6700, totalRevenue: 8900000, joinedDate: "2023-04-08", status: "active", category: "Fashion" },
  { id: "v-6", userId: "u-10", storeName: "HomeEssentials", logo: "🏡", description: "Everything for your home", rating: 4.2, totalProducts: 340, totalOrders: 4500, totalRevenue: 23400000, joinedDate: "2023-06-30", status: "active", category: "Home & Living" },
  { id: "v-7", userId: "u-11", storeName: "BookHaven", logo: "📖", description: "Your online bookstore", rating: 4.7, totalProducts: 5600, totalOrders: 34000, totalRevenue: 4500000, joinedDate: "2022-11-15", status: "active", category: "Books" },
  { id: "v-8", userId: "u-12", storeName: "GlamourBox", logo: "💅", description: "Beauty & cosmetics", rating: 4.1, totalProducts: 450, totalOrders: 7800, totalRevenue: 5600000, joinedDate: "2023-07-22", status: "pending", category: "Beauty" },
  { id: "v-9", userId: "u-13", storeName: "NaturalFoods", logo: "🌿", description: "Organic & natural groceries", rating: 4.0, totalProducts: 890, totalOrders: 12000, totalRevenue: 3400000, joinedDate: "2023-08-05", status: "active", category: "Groceries" },
  { id: "v-10", userId: "u-14", storeName: "ToyLand", logo: "🧸", description: "Toys & games for all ages", rating: 4.6, totalProducts: 670, totalOrders: 5600, totalRevenue: 7800000, joinedDate: "2023-09-12", status: "suspended", category: "Toys & Games" },
];

export const mockCredentials = [
  { email: "rahul@example.com", password: "password", userId: "u-1" },
  { email: "priya@vendor.com", password: "password", userId: "u-2" },
  { email: "admin@marketplace.com", password: "password", userId: "u-3" },
  { email: "anita@example.com", password: "password", userId: "u-4" },
  { email: "vikram@example.com", password: "password", userId: "u-5" },
];
