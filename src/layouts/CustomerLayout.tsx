import { Outlet, Link, useNavigate } from "react-router-dom";
import { useStore } from "@/store/useStore";
import { useNotificationStore } from "@/store/notificationStore";
import { SearchBar } from "@/components/shared/SearchBar";
import { RoleSwitcher } from "@/components/shared/RoleSwitcher";
import { CompareBar } from "@/components/shared/CompareBar";
import { ThemeToggle } from "@/components/shared/ThemeToggle";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { ShoppingCart, Heart, User, ChevronDown, MapPin, Bell, LogOut, Package, Settings, Store } from "lucide-react";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger, DropdownMenuLabel,
} from "@/components/ui/dropdown-menu";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger,
} from "@/components/ui/dialog";
import { categories } from "@/data/mock-products";
import { useState } from "react";

const deliverablePincodes: Record<string, string> = {
  "400001": "Mumbai", "400002": "Mumbai", "560001": "Bangalore", "560066": "Bangalore",
  "110001": "Delhi", "411001": "Pune", "500001": "Hyderabad", "600001": "Chennai", "700001": "Kolkata",
};

export default function CustomerLayout() {
  const { cartCount, currentUser, isAuthenticated, logout, currentRole } = useStore();
  const { unreadCount } = useNotificationStore();
  const navigate = useNavigate();
  const count = cartCount();
  const unread = unreadCount();
  const [locationPincode, setLocationPincode] = useState("400001");
  const [locationCity, setLocationCity] = useState("Mumbai");
  const [tempPincode, setTempPincode] = useState("");
  const [locationOpen, setLocationOpen] = useState(false);

  const handleLocationChange = () => {
    const city = deliverablePincodes[tempPincode];
    if (city) {
      setLocationPincode(tempPincode);
      setLocationCity(city);
      setLocationOpen(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const handleSellOnMarketHub = () => {
    if (!isAuthenticated) {
      navigate("/login", { state: { from: "/vendor/register" } });
      return;
    }
    const status = currentUser?.vendorStatus ?? "none";
    if (currentUser?.isVendor || status === "active" || status === "approved") {
      navigate("/vendor");
    } else if (status === "pending") {
      navigate("/vendor/register/success");
    } else {
      navigate("/vendor/register");
    }
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Top bar */}
      <div className="gradient-primary text-primary-foreground">
        <div className="container flex items-center justify-between py-1.5 text-xs">
          <div className="flex items-center gap-4">
            <Dialog open={locationOpen} onOpenChange={setLocationOpen}>
              <DialogTrigger asChild>
                <button className="flex items-center gap-1 hover:opacity-80 transition-opacity">
                  <MapPin className="h-3 w-3" /> Deliver to {locationCity} {locationPincode}
                </button>
              </DialogTrigger>
              <DialogContent className="max-w-xs">
                <DialogHeader><DialogTitle>Change Delivery Location</DialogTitle></DialogHeader>
                <div className="space-y-3 pt-2">
                  <Input placeholder="Enter 6-digit pincode" value={tempPincode} onChange={e => setTempPincode(e.target.value.replace(/\D/g, "").slice(0, 6))} maxLength={6} />
                  <Button className="w-full" onClick={handleLocationChange} disabled={tempPincode.length !== 6}>Update Location</Button>
                  <p className="text-xs text-muted-foreground">Try: 400001, 560001, 110001, 411001</p>
                </div>
              </DialogContent>
            </Dialog>
          </div>
          <RoleSwitcher />
        </div>
      </div>

      {/* Main header */}
      <header className="sticky top-0 z-40 bg-card border-b shadow-card">
        <div className="container flex items-center gap-4 py-3">
          <Link to="/" className="shrink-0">
            <h1 className="text-xl font-display font-bold text-gradient">MarketHub</h1>
          </Link>
          <SearchBar />
          <div className="flex items-center gap-1">
            {!isAuthenticated ? (
              <Button size="sm" onClick={() => navigate("/login")}>Login</Button>
            ) : (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="sm" className="gap-1">
                    <div className="h-6 w-6 rounded-full bg-primary/10 flex items-center justify-center text-[10px] font-semibold text-primary">
                      {currentUser?.name?.[0] || "U"}
                    </div>
                    <span className="hidden md:inline text-xs">{currentUser?.name?.split(" ")[0]}</span>
                    <ChevronDown className="h-3 w-3" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-52">
                  <DropdownMenuLabel className="font-normal">
                    <p className="text-sm font-medium">{currentUser?.name}</p>
                    <p className="text-xs text-muted-foreground">{currentUser?.email}</p>
                    <Badge variant="outline" className="text-[10px] mt-1 capitalize">{currentRole}</Badge>
                  </DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onClick={() => navigate("/profile")}>
                    <User className="mr-2 h-4 w-4" /> My Profile
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => navigate("/orders")}>
                    <Package className="mr-2 h-4 w-4" /> My Orders
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => navigate("/wishlist")}>
                    <Heart className="mr-2 h-4 w-4" /> Wishlist
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => navigate("/notifications")}>
                    <Bell className="mr-2 h-4 w-4" /> Notifications
                    {unread > 0 && <Badge className="ml-auto h-4 text-[10px]">{unread}</Badge>}
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onClick={handleSellOnMarketHub}>
                    <Store className="mr-2 h-4 w-4" /> Sell on MarketHub
                    {currentUser?.vendorStatus === "pending" && <Badge variant="outline" className="ml-auto text-[9px]">Pending</Badge>}
                    {(currentUser?.isVendor || currentUser?.vendorStatus === "active") && <Badge className="ml-auto text-[9px] bg-success/10 text-success border-0">Active</Badge>}
                  </DropdownMenuItem>
                  {(currentRole === "vendor" || currentRole === "admin") && (
                    <>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem onClick={() => navigate(currentRole === "vendor" ? "/vendor" : "/admin")}>
                        <Settings className="mr-2 h-4 w-4" /> {currentRole === "vendor" ? "Vendor Dashboard" : "Admin Panel"}
                      </DropdownMenuItem>
                    </>
                  )}
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onClick={handleLogout} className="text-destructive focus:text-destructive">
                    <LogOut className="mr-2 h-4 w-4" /> Logout
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            )}

            <ThemeToggle />

            {/* Notifications bell */}
            <Button variant="ghost" size="icon" onClick={() => navigate("/notifications")} className="relative">
              <Bell className="h-4 w-4" />
              {unread > 0 && (
                <Badge className="absolute -top-1 -right-1 h-4 min-w-4 p-0 flex items-center justify-center text-[10px] bg-destructive text-destructive-foreground">
                  {unread}
                </Badge>
              )}
            </Button>

            <Button variant="ghost" size="icon" onClick={() => navigate("/wishlist")} className="relative">
              <Heart className="h-4 w-4" />
            </Button>

            <Button variant="ghost" size="icon" onClick={() => navigate("/cart")} className="relative">
              <ShoppingCart className="h-4 w-4" />
              {count > 0 && (
                <Badge className="absolute -top-1 -right-1 h-4 min-w-4 p-0 flex items-center justify-center text-[10px] bg-secondary text-secondary-foreground">
                  {count}
                </Badge>
              )}
            </Button>
          </div>
        </div>

        {/* Categories nav */}
        <div className="border-t">
          <div className="container overflow-x-auto">
            <div className="flex items-center gap-1 py-1.5">
              {categories.map(cat => (
                <Link
                  key={cat.id}
                  to={`/products?category=${cat.slug}`}
                  className="flex items-center gap-1.5 whitespace-nowrap rounded-full px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
                >
                  <span>{cat.icon}</span>
                  <span>{cat.name}</span>
                </Link>
              ))}
            </div>
          </div>
        </div>
      </header>

      <main><Outlet /></main>
      <CompareBar />

      {/* Footer */}
      <footer className="mt-12 border-t bg-card">
        <div className="container py-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            <div>
              <h3 className="font-display font-semibold mb-3">About</h3>
              <div className="space-y-2 text-sm text-muted-foreground">
                <p><Link to="/about" className="hover:text-foreground transition-colors">About Us</Link></p>
                <p><Link to="/contact" className="hover:text-foreground transition-colors">Contact</Link></p>
                <p><Link to="/vendor/register" className="hover:text-foreground transition-colors">Sell on MarketHub</Link></p>
              </div>
            </div>
            <div>
              <h3 className="font-display font-semibold mb-3">Help</h3>
              <div className="space-y-2 text-sm text-muted-foreground">
                <p><Link to="/faq" className="hover:text-foreground transition-colors">FAQ</Link></p>
                <p><Link to="/contact" className="hover:text-foreground transition-colors">Shipping Info</Link></p>
                <p><Link to="/faq" className="hover:text-foreground transition-colors">Returns</Link></p>
              </div>
            </div>
            <div>
              <h3 className="font-display font-semibold mb-3">Policy</h3>
              <div className="space-y-2 text-sm text-muted-foreground">
                <p><Link to="/privacy" className="hover:text-foreground transition-colors">Privacy Policy</Link></p>
                <p><Link to="/terms" className="hover:text-foreground transition-colors">Terms of Service</Link></p>
                <p><Link to="/privacy" className="hover:text-foreground transition-colors">Security</Link></p>
              </div>
            </div>
            <div>
              <h3 className="font-display font-semibold mb-3">Connect</h3>
              <div className="space-y-2 text-sm text-muted-foreground">
                <p><a href="https://twitter.com" target="_blank" rel="noopener noreferrer" className="hover:text-foreground transition-colors">Twitter</a></p>
                <p><a href="https://instagram.com" target="_blank" rel="noopener noreferrer" className="hover:text-foreground transition-colors">Instagram</a></p>
                <p><a href="https://facebook.com" target="_blank" rel="noopener noreferrer" className="hover:text-foreground transition-colors">Facebook</a></p>
              </div>
            </div>
          </div>
          <div className="mt-8 pt-6 border-t text-center text-sm text-muted-foreground">
            © 2025 MarketHub. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
}
