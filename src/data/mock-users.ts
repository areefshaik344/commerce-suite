export type UserRole = "customer" | "vendor" | "admin";

export type VendorStatus = "none" | "pending" | "approved" | "active" | "rejected";

export interface User {
  id: string;
  name: string;
  email: string;
  avatar: string;
  role: UserRole;
  phone: string;
  joinedDate: string;
  addresses?: Address[];
  isVendor?: boolean;
  vendorStatus?: VendorStatus;
}

export interface Address {
  id: string;
  label: string;
  name: string;
  phone: string;
  line1: string;
  line2: string;
  city: string;
  state: string;
  pincode: string;
  isDefault: boolean;
}

export interface Vendor {
  id: string;
  userId: string;
  storeName: string;
  logo: string;
  description: string;
  rating: number;
  totalProducts: number;
  totalOrders: number;
  totalRevenue: number;
  joinedDate: string;
  status: "active" | "pending" | "suspended";
  category: string;
}

export const users: User[] = [
  { id: "u-1", name: "Rahul Sharma", email: "rahul@example.com", avatar: "", role: "customer", phone: "+91 98765 43210", joinedDate: "2023-06-15", isVendor: false, vendorStatus: "none", addresses: [
    { id: "a-1", label: "Home", name: "Rahul Sharma", phone: "+91 98765 43210", line1: "42, Marine Drive Apartments", line2: "Near Gateway of India", city: "Mumbai", state: "Maharashtra", pincode: "400001", isDefault: true },
    { id: "a-2", label: "Office", name: "Rahul Sharma", phone: "+91 98765 43211", line1: "Tech Park, Tower B, 5th Floor", line2: "Whitefield", city: "Bangalore", state: "Karnataka", pincode: "560066", isDefault: false },
  ]},
  { id: "u-2", name: "Priya Patel", email: "priya@vendor.com", avatar: "", role: "vendor", phone: "+91 87654 32109", joinedDate: "2023-03-10", isVendor: true, vendorStatus: "active" },
  { id: "u-3", name: "Admin User", email: "admin@marketplace.com", avatar: "", role: "admin", phone: "+91 99999 00000", joinedDate: "2022-01-01" },
  { id: "u-4", name: "Anita Singh", email: "anita@example.com", avatar: "", role: "customer", phone: "+91 76543 21098", joinedDate: "2023-09-22", isVendor: false, vendorStatus: "none" },
  { id: "u-5", name: "Vikram Joshi", email: "vikram@example.com", avatar: "", role: "customer", phone: "+91 65432 10987", joinedDate: "2024-01-05", isVendor: false, vendorStatus: "none" },
];

export const vendors: Vendor[] = [
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
