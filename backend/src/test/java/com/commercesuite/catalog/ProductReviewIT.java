package com.commercesuite.catalog;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.catalog.dto.CreateProductRequest;
import com.commercesuite.catalog.service.CategoryService;
import com.commercesuite.catalog.service.ProductModerationService;
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
class ProductReviewIT extends AbstractIT {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired RoleService roles;
    @Autowired VendorApplicationService apps;
    @Autowired VendorAdminService vendorAdmin;
    @Autowired ProductService productService;
    @Autowired ProductModerationService moderation;
    @Autowired CategoryService categories;

    @Test
    void customerCanReviewApprovedProductOnce() throws Exception {
        var vendor = CatalogTestSupport.approvedVendor(auth, roles, apps, vendorAdmin, "vr");
        var admin = VendorTestSupport.signupAdmin(auth, roles, "radm");
        var catId = CatalogTestSupport.newCategory(categories, "Catr " + System.nanoTime());

        var p = productService.create(new com.commercesuite.common.audit.ActorContext(vendor.userId(),
                java.util.Set.of(), java.util.Set.of("MANAGE_PRODUCTS"), "test"),
                new CreateProductRequest(catId, null, null, "Mug", "a", "b"));
        productService.submit(new com.commercesuite.common.audit.ActorContext(vendor.userId(),
                java.util.Set.of(), java.util.Set.of("MANAGE_PRODUCTS"), "test"), p.id());
        moderation.approve(p.id(), admin.userId(), "ok");

        var customer = VendorTestSupport.signupCustomer(auth, "rcust");
        String body = "{\"rating\":5,\"title\":\"Great\",\"reviewText\":\"Loved it\"}";

        mvc.perform(post("/api/v1/products/" + p.id() + "/reviews")
                .header("Authorization", "Bearer " + customer.token())
                .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5));

        // Duplicate review -> 409
        mvc.perform(post("/api/v1/products/" + p.id() + "/reviews")
                .header("Authorization", "Bearer " + customer.token())
                .contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }
}