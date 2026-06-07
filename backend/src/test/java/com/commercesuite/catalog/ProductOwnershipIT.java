package com.commercesuite.catalog;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.catalog.dto.CreateProductRequest;
import com.commercesuite.catalog.service.BrandService;
import com.commercesuite.catalog.service.CategoryService;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.vendor.service.VendorAdminService;
import com.commercesuite.vendor.service.VendorApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ProductOwnershipIT extends AbstractIT {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired RoleService roles;
    @Autowired VendorApplicationService apps;
    @Autowired VendorAdminService vendorAdmin;
    @Autowired CategoryService categories;
    @Autowired BrandService brands;
    @Autowired ObjectMapper mapper;

    @Test
    void vendorCanCreateAndOtherVendorCannotAccess() throws Exception {
        var v1 = CatalogTestSupport.approvedVendor(auth, roles, apps, vendorAdmin, "v1");
        var v2 = CatalogTestSupport.approvedVendor(auth, roles, apps, vendorAdmin, "v2");
        var catId = CatalogTestSupport.newCategory(categories, "Tools " + System.nanoTime());

        String body = mapper.writeValueAsString(new CreateProductRequest(catId, null, null,
                "Hammer", "A solid hammer", "Steel head"));

        var created = mvc.perform(post("/api/v1/products")
                .header("Authorization", "Bearer " + v1.token())
                .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = mapper.readTree(created).path("data").path("id").asText();

        mvc.perform(get("/api/v1/products/" + id)
                .header("Authorization", "Bearer " + v2.token()))
                .andExpect(status().isForbidden());
    }
}