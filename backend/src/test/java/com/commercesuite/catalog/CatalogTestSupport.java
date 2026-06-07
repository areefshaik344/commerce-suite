package com.commercesuite.catalog;

import com.commercesuite.auth.service.AuthService;
import com.commercesuite.catalog.dto.UpsertBrandRequest;
import com.commercesuite.catalog.dto.UpsertCategoryRequest;
import com.commercesuite.catalog.entity.Brand;
import com.commercesuite.catalog.entity.Category;
import com.commercesuite.catalog.repository.BrandRepository;
import com.commercesuite.catalog.repository.CategoryRepository;
import com.commercesuite.catalog.service.BrandService;
import com.commercesuite.catalog.service.CategoryService;
import com.commercesuite.rbac.entity.AppRole;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.vendor.VendorTestSupport;
import com.commercesuite.vendor.VendorTestSupport.TestUser;
import com.commercesuite.vendor.service.VendorAdminService;
import com.commercesuite.vendor.service.VendorApplicationService;
import java.util.UUID;

public final class CatalogTestSupport {
    private CatalogTestSupport() {}

    /** Sign up a customer, apply as vendor, approve as admin, return APPROVED vendor login. */
    public static TestUser approvedVendor(AuthService auth, RoleService roles,
                                          VendorApplicationService apps, VendorAdminService vendorAdmin,
                                          String prefix) {
        var customer = VendorTestSupport.signupCustomer(auth, prefix);
        var app = apps.apply(customer.userId(), VendorTestSupport.sampleApply());
        var admin = VendorTestSupport.signupAdmin(auth, roles, prefix + "-adm");
        vendorAdmin.approve(app.vendorId(), admin.userId(), "ok");
        // Re-login so JWT carries VENDOR role.
        var login = auth.login(new com.commercesuite.auth.dto.LoginRequest(customer.email(), "Str0ng!Pwd"),
                "ua", "127.0.0.1");
        return new TestUser(customer.email(), login.tokens().accessToken(), customer.userId());
    }

    public static Category category(CategoryService svc, String name) {
        var dto = svc.create(new UpsertCategoryRequest(null, name, null, null, null, 0, true));
        return Category.builder().build().toBuilder().build(); // never used; tests fetch via repo
    }

    public static UUID newCategory(CategoryService svc, String name) {
        return svc.create(new UpsertCategoryRequest(null, name, null, null, null, 0, true)).id();
    }
    public static UUID newBrand(BrandService svc, String name) {
        return svc.create(new UpsertBrandRequest(name, null, null, null, true)).id();
    }
}