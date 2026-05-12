import { lazy, Suspense, useEffect } from "react";
import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { LayoutDashboard, Package, ShoppingCart, Star, DollarSign, Settings, Users, Store, Tag, BarChart3, Image, Ticket, Truck, Archive, ClipboardCheck, Percent, ShieldAlert, FileBarChart, RotateCcw, Upload, Palette, FileText, Mail, Megaphone, LifeBuoy, Activity } from "lucide-react";

import { ErrorBoundary } from "@/components/shared/ErrorBoundary";
import ProtectedRoute from "@/routes/ProtectedRoute";
import PublicRoute from "@/routes/PublicRoute";
import AuthLoader from "@/components/auth/AuthLoader";
import SessionExpiredDialog from "@/components/auth/SessionExpiredDialog";
import { useAuthStore } from "@/store/authStore";
import CustomerLayout from "@/layouts/CustomerLayout";
import DashboardLayout from "@/layouts/DashboardLayout";

// Lazy-loaded pages
const LoginPage = lazy(() => import("@/pages/auth/LoginPage"));
const SignupPage = lazy(() => import("@/pages/auth/SignupPage"));
const ForgotPasswordPage = lazy(() => import("@/pages/auth/ForgotPasswordPage"));
const ResetPasswordPage = lazy(() => import("@/pages/auth/ResetPasswordPage"));
const VendorRegisterPage = lazy(() => import("@/pages/auth/VendorRegisterPage"));
const VendorRegisterSuccessPage = lazy(() => import("@/pages/auth/VendorRegisterSuccessPage"));
const EmailVerificationPage = lazy(() => import("@/pages/auth/EmailVerificationPage"));

const HomePage = lazy(() => import("@/pages/customer/HomePage"));
const ProductsPage = lazy(() => import("@/pages/customer/ProductsPage"));
const ProductDetailPage = lazy(() => import("@/pages/customer/ProductDetailPage"));
const CartPage = lazy(() => import("@/pages/customer/CartPage"));
const CheckoutPage = lazy(() => import("@/pages/customer/CheckoutPage"));
const OrderSuccessPage = lazy(() => import("@/pages/customer/OrderSuccessPage"));
const OrdersPage = lazy(() => import("@/pages/customer/OrdersPage"));
const OrderDetailPage = lazy(() => import("@/pages/customer/OrderDetailPage"));
const WishlistPage = lazy(() => import("@/pages/customer/WishlistPage"));
const ProfilePage = lazy(() => import("@/pages/customer/ProfilePage"));
const NotificationsPage = lazy(() => import("@/pages/customer/NotificationsPage"));
const VendorStorePage = lazy(() => import("@/pages/customer/VendorStorePage"));
const ComparePage = lazy(() => import("@/pages/customer/ComparePage"));

const AboutPage = lazy(() => import("@/pages/static/AboutPage"));
const ContactPage = lazy(() => import("@/pages/static/ContactPage"));
const FAQPage = lazy(() => import("@/pages/static/FAQPage"));
const PrivacyPage = lazy(() => import("@/pages/static/PrivacyPage"));
const TermsPage = lazy(() => import("@/pages/static/TermsPage"));

const VendorDashboard = lazy(() => import("@/pages/vendor/VendorDashboard"));
const VendorProducts = lazy(() => import("@/pages/vendor/VendorProducts"));
const VendorProductForm = lazy(() => import("@/pages/vendor/VendorProductForm"));
const VendorProductEdit = lazy(() => import("@/pages/vendor/VendorProductEdit"));
const VendorOrders = lazy(() => import("@/pages/vendor/VendorOrders"));
const VendorOrderDetail = lazy(() => import("@/pages/vendor/VendorOrderDetail"));
const VendorReviews = lazy(() => import("@/pages/vendor/VendorReviews"));
const VendorFinancials = lazy(() => import("@/pages/vendor/VendorFinancials"));
const VendorPayoutHistory = lazy(() => import("@/pages/vendor/VendorPayoutHistory"));
const VendorSettings = lazy(() => import("@/pages/vendor/VendorSettings"));
const VendorCoupons = lazy(() => import("@/pages/vendor/VendorCoupons"));
const VendorCreateCoupon = lazy(() => import("@/pages/vendor/VendorCreateCoupon"));
const VendorOnboarding = lazy(() => import("@/pages/vendor/VendorOnboarding"));
const VendorInventory = lazy(() => import("@/pages/vendor/VendorInventory"));
const VendorLowStockAlerts = lazy(() => import("@/pages/vendor/VendorLowStockAlerts"));
const VendorShipping = lazy(() => import("@/pages/vendor/VendorShipping"));
const VendorAnalytics = lazy(() => import("@/pages/vendor/VendorAnalytics"));
const VendorReturns = lazy(() => import("@/pages/vendor/VendorReturns"));
const VendorBulkUpload = lazy(() => import("@/pages/vendor/VendorBulkUpload"));
const VendorStoreCustomization = lazy(() => import("@/pages/vendor/VendorStoreCustomization"));
const VendorPerformance = lazy(() => import("@/pages/vendor/VendorPerformance"));
const VendorAds = lazy(() => import("@/pages/vendor/VendorAds"));
const VendorDisputes = lazy(() => import("@/pages/vendor/VendorDisputes"));
const VendorTickets = lazy(() => import("@/pages/vendor/VendorTickets"));

const AdminDashboard = lazy(() => import("@/pages/admin/AdminDashboard"));
const AdminUsers = lazy(() => import("@/pages/admin/AdminUsers"));
const AdminUserDetail = lazy(() => import("@/pages/admin/AdminUserDetail"));
const AdminVendors = lazy(() => import("@/pages/admin/AdminVendors"));
const AdminVendorDetail = lazy(() => import("@/pages/admin/AdminVendorDetail"));
const AdminVendorApplications = lazy(() => import("@/pages/admin/AdminVendorApplications"));
const AdminProducts = lazy(() => import("@/pages/admin/AdminProducts"));
const AdminProductDetail = lazy(() => import("@/pages/admin/AdminProductDetail"));
const AdminOrders = lazy(() => import("@/pages/admin/AdminOrders"));
const AdminOrderDetail = lazy(() => import("@/pages/admin/AdminOrderDetail"));
const AdminCategories = lazy(() => import("@/pages/admin/AdminCategories"));
const AdminAnalytics = lazy(() => import("@/pages/admin/AdminAnalytics"));
const AdminCMS = lazy(() => import("@/pages/admin/AdminCMS"));
const AdminBannerForm = lazy(() => import("@/pages/admin/AdminBannerForm"));
const AdminSettings = lazy(() => import("@/pages/admin/AdminSettings"));
const AdminCoupons = lazy(() => import("@/pages/admin/AdminCoupons"));
const AdminCreateCoupon = lazy(() => import("@/pages/admin/AdminCreateCoupon"));
const AdminCommission = lazy(() => import("@/pages/admin/AdminCommission"));
const AdminFraud = lazy(() => import("@/pages/admin/AdminFraud"));
const AdminReporting = lazy(() => import("@/pages/admin/AdminReporting"));
const AdminAuditLog = lazy(() => import("@/pages/admin/AdminAuditLog"));
const AdminEmailTemplates = lazy(() => import("@/pages/admin/AdminEmailTemplates"));

const NotFound = lazy(() => import("@/pages/NotFound"));

const queryClient = new QueryClient();

function PageLoader() {
  return (
    <div className="flex items-center justify-center min-h-[400px]">
      <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
    </div>
  );
}

function AuthBootstrapGate({ children }: { children: React.ReactNode }) {
  const isBootstrapping = useAuthStore((s) => s.isBootstrapping);
  const bootstrap = useAuthStore((s) => s.bootstrap);
  useEffect(() => { void bootstrap(); }, [bootstrap]);
  if (isBootstrapping) return <AuthLoader />;
  return <>{children}</>;
}

const vendorNav = [
  { title: "Dashboard", url: "/vendor", icon: LayoutDashboard },
  { title: "Onboarding", url: "/vendor/onboarding", icon: ClipboardCheck },
  { title: "Products", url: "/vendor/products", icon: Package },
  { title: "Bulk Upload", url: "/vendor/products/bulk-upload", icon: Upload },
  { title: "Inventory", url: "/vendor/inventory", icon: Archive },
  { title: "Orders", url: "/vendor/orders", icon: ShoppingCart },
  { title: "Returns", url: "/vendor/returns", icon: RotateCcw },
  { title: "Disputes", url: "/vendor/disputes", icon: ShieldAlert },
  { title: "Shipping", url: "/vendor/shipping", icon: Truck },
  { title: "Coupons", url: "/vendor/coupons", icon: Tag },
  { title: "Ads", url: "/vendor/ads", icon: Megaphone },
  { title: "Reviews", url: "/vendor/reviews", icon: Star },
  { title: "Performance", url: "/vendor/performance", icon: Activity },
  { title: "Analytics", url: "/vendor/analytics", icon: BarChart3 },
  { title: "Financials", url: "/vendor/financials", icon: DollarSign },
  { title: "Store Design", url: "/vendor/store-customization", icon: Palette },
  { title: "Help & Tickets", url: "/vendor/tickets", icon: LifeBuoy },
  { title: "Settings", url: "/vendor/settings", icon: Settings },
];

const adminNav = [
  { title: "Dashboard", url: "/admin", icon: LayoutDashboard },
  { title: "Users", url: "/admin/users", icon: Users },
  { title: "Vendors", url: "/admin/vendors", icon: Store },
  { title: "Applications", url: "/admin/vendor-applications", icon: ClipboardCheck },
  { title: "Products", url: "/admin/products", icon: Package },
  { title: "Orders", url: "/admin/orders", icon: ShoppingCart },
  { title: "Categories", url: "/admin/categories", icon: Tag },
  { title: "Commission", url: "/admin/commission", icon: Percent },
  { title: "Coupons", url: "/admin/coupons", icon: Ticket },
  { title: "Fraud", url: "/admin/fraud", icon: ShieldAlert },
  { title: "Reporting", url: "/admin/reporting", icon: FileBarChart },
  { title: "Audit Log", url: "/admin/audit-log", icon: FileText },
  { title: "Analytics", url: "/admin/analytics", icon: BarChart3 },
  { title: "CMS", url: "/admin/cms", icon: Image },
  { title: "Emails", url: "/admin/email-templates", icon: Mail },
  { title: "Settings", url: "/admin/settings", icon: Settings },
];

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <ErrorBoundary>
        <Toaster />
        <Sonner />
        <BrowserRouter>
          <SessionExpiredDialog />
          <AuthBootstrapGate>
          <Suspense fallback={<PageLoader />}>
            <Routes>
              {/* Auth - public routes */}
              <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
              <Route path="/signup" element={<PublicRoute><SignupPage /></PublicRoute>} />
              <Route path="/verify-email" element={<EmailVerificationPage />} />
              <Route path="/forgot-password" element={<PublicRoute><ForgotPasswordPage /></PublicRoute>} />
              <Route path="/reset-password" element={<PublicRoute><ResetPasswordPage /></PublicRoute>} />
              <Route path="/vendor/register" element={<VendorRegisterPage />} />
              <Route path="/vendor/register/success" element={<VendorRegisterSuccessPage />} />

              {/* Customer - public browsing, auth for checkout/profile */}
              <Route element={<CustomerLayout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/products" element={<ProductsPage />} />
                <Route path="/product/:slug" element={<ProductDetailPage />} />
                <Route path="/cart" element={<CartPage />} />
                <Route path="/store/:vendorSlug" element={<VendorStorePage />} />
                <Route path="/compare" element={<ComparePage />} />
                <Route path="/about" element={<AboutPage />} />
                <Route path="/contact" element={<ContactPage />} />
                <Route path="/faq" element={<FAQPage />} />
                <Route path="/privacy" element={<PrivacyPage />} />
                <Route path="/terms" element={<TermsPage />} />
                <Route path="/checkout" element={<ProtectedRoute allowedRoles={["customer"]}><CheckoutPage /></ProtectedRoute>} />
                <Route path="/order-success" element={<ProtectedRoute allowedRoles={["customer"]}><OrderSuccessPage /></ProtectedRoute>} />
                <Route path="/orders" element={<ProtectedRoute allowedRoles={["customer"]}><OrdersPage /></ProtectedRoute>} />
                <Route path="/orders/:id" element={<ProtectedRoute allowedRoles={["customer"]}><OrderDetailPage /></ProtectedRoute>} />
                <Route path="/wishlist" element={<WishlistPage />} />
                <Route path="/profile" element={<ProtectedRoute allowedRoles={["customer", "vendor", "admin"]}><ProfilePage /></ProtectedRoute>} />
                <Route path="/notifications" element={<ProtectedRoute allowedRoles={["customer"]}><NotificationsPage /></ProtectedRoute>} />
              </Route>

              {/* Vendor - protected */}
              <Route element={
                <ProtectedRoute allowedRoles={["vendor", "admin"]}>
                  <DashboardLayout title="Vendor Portal" navItems={vendorNav} />
                </ProtectedRoute>
              }>
                <Route path="/vendor" element={<VendorDashboard />} />
                <Route path="/vendor/products" element={<VendorProducts />} />
                <Route path="/vendor/products/new" element={<VendorProductForm />} />
                <Route path="/vendor/products/:id/edit" element={<VendorProductEdit />} />
                <Route path="/vendor/products/bulk-upload" element={<VendorBulkUpload />} />
                <Route path="/vendor/inventory" element={<VendorInventory />} />
                <Route path="/vendor/inventory/low-stock" element={<VendorLowStockAlerts />} />
                <Route path="/vendor/orders" element={<VendorOrders />} />
                <Route path="/vendor/orders/:id" element={<VendorOrderDetail />} />
                <Route path="/vendor/returns" element={<VendorReturns />} />
                <Route path="/vendor/shipping" element={<VendorShipping />} />
                <Route path="/vendor/reviews" element={<VendorReviews />} />
                <Route path="/vendor/analytics" element={<VendorAnalytics />} />
                <Route path="/vendor/coupons" element={<VendorCoupons />} />
                <Route path="/vendor/coupons/new" element={<VendorCreateCoupon />} />
                <Route path="/vendor/financials" element={<VendorFinancials />} />
                <Route path="/vendor/financials/payouts" element={<VendorPayoutHistory />} />
                <Route path="/vendor/store-customization" element={<VendorStoreCustomization />} />
                <Route path="/vendor/onboarding" element={<VendorOnboarding />} />
                <Route path="/vendor/performance" element={<VendorPerformance />} />
                <Route path="/vendor/ads" element={<VendorAds />} />
                <Route path="/vendor/disputes" element={<VendorDisputes />} />
                <Route path="/vendor/tickets" element={<VendorTickets />} />
                <Route path="/vendor/settings" element={<VendorSettings />} />
              </Route>

              {/* Admin - protected */}
              <Route element={
                <ProtectedRoute allowedRoles={["admin"]}>
                  <DashboardLayout title="Admin Portal" navItems={adminNav} />
                </ProtectedRoute>
              }>
                <Route path="/admin" element={<AdminDashboard />} />
                <Route path="/admin/users" element={<AdminUsers />} />
                <Route path="/admin/users/:id" element={<AdminUserDetail />} />
                <Route path="/admin/vendors" element={<AdminVendors />} />
                <Route path="/admin/vendors/:id" element={<AdminVendorDetail />} />
                <Route path="/admin/vendor-applications" element={<AdminVendorApplications />} />
                <Route path="/admin/products" element={<AdminProducts />} />
                <Route path="/admin/products/:id" element={<AdminProductDetail />} />
                <Route path="/admin/orders" element={<AdminOrders />} />
                <Route path="/admin/orders/:id" element={<AdminOrderDetail />} />
                <Route path="/admin/categories" element={<AdminCategories />} />
                <Route path="/admin/commission" element={<AdminCommission />} />
                <Route path="/admin/coupons" element={<AdminCoupons />} />
                <Route path="/admin/coupons/new" element={<AdminCreateCoupon />} />
                <Route path="/admin/fraud" element={<AdminFraud />} />
                <Route path="/admin/reporting" element={<AdminReporting />} />
                <Route path="/admin/audit-log" element={<AdminAuditLog />} />
                <Route path="/admin/analytics" element={<AdminAnalytics />} />
                <Route path="/admin/cms" element={<AdminCMS />} />
                <Route path="/admin/cms/banners/new" element={<AdminBannerForm />} />
                <Route path="/admin/cms/banners/:id/edit" element={<AdminBannerForm />} />
                <Route path="/admin/email-templates" element={<AdminEmailTemplates />} />
                <Route path="/admin/settings" element={<AdminSettings />} />
              </Route>

              <Route path="*" element={<NotFound />} />
            </Routes>
          </Suspense>
          </AuthBootstrapGate>
        </BrowserRouter>
      </ErrorBoundary>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
