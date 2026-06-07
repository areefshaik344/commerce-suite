package com.commercesuite.catalog;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.catalog.dto.CreateProductRequest;
import com.commercesuite.catalog.service.CategoryService;
import com.commercesuite.catalog.service.ProductService;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.vendor.VendorTestSupport;
import com.commercesuite.vendor.service.VendorAdminService;
import com.commercesuite.vendor.service.VendorApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ProductModerationIT extends AbstractIT {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired RoleService roles;
    @Autowired VendorApplicationService apps;
    @Autowired VendorAdminService vendorAdmin;
    @Autowired ProductService productService;
    @Autowired CategoryService categories;

    @Test
    void submitApproveAndIllegalReApproveIs409() throws Exception {
        var vendor = CatalogTestSupport.approvedVendor(auth, roles, apps, vendorAdmin, "vm");
        var admin = VendorTestSupport.signupAdmin(auth, roles, "modadm");
        var catId = CatalogTestSupport.newCategory(categories, "Cat " + System.nanoTime());

        var p = productService.create(
                new com.commercesuite.common.audit.ActorContext(vendor.userId(),
                        java.util.Set.of("CUSTOMER","VENDOR"),
                        java.util.Set.of("MANAGE_PRODUCTS"), "test"),
                new CreateProductRequest(catId, null, null, "Widget", "x", "y"));

        mvc.perform(post("/api/v1/products/" + p.id() + "/submit")
                .header("Authorization", "Bearer " + vendor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));

        mvc.perform(post("/api/v1/admin/products/" + p.id() + "/approve")
                .header("Authorization", "Bearer " + admin.token())
                .contentType("application/json").content("{\"reason\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mvc.perform(post("/api/v1/admin/products/" + p.id() + "/approve")
                .header("Authorization", "Bearer " + admin.token())
                .contentType("application/json").content("{\"reason\":\"again\"}"))
                .andExpect(status().isConflict());
    }
}