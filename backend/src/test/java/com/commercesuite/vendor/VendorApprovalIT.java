package com.commercesuite.vendor;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.rbac.service.RoleService;
import com.commercesuite.vendor.service.VendorApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class VendorApprovalIT extends AbstractIT {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired RoleService roles;
    @Autowired VendorApplicationService applicationService;
    @Autowired ObjectMapper mapper;

    @Test
    void adminApprovesVendorAndIllegalTransitionsAre409() throws Exception {
        var customer = VendorTestSupport.signupCustomer(auth, "vendor");
        var app = applicationService.apply(customer.userId(), VendorTestSupport.sampleApply());

        var admin = VendorTestSupport.signupAdmin(auth, roles, "admin");
        String body = "{\"reason\":\"docs ok\"}";

        // Approve
        mvc.perform(post("/api/v1/admin/vendors/" + app.vendorId() + "/approve")
                .header("Authorization", "Bearer " + admin.token())
                .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Cannot approve again
        mvc.perform(post("/api/v1/admin/vendors/" + app.vendorId() + "/approve")
                .header("Authorization", "Bearer " + admin.token())
                .contentType("application/json").content(body))
                .andExpect(status().isConflict());

        // Suspend then reactivate is allowed
        mvc.perform(post("/api/v1/admin/vendors/" + app.vendorId() + "/suspend")
                .header("Authorization", "Bearer " + admin.token())
                .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        mvc.perform(post("/api/v1/admin/vendors/" + app.vendorId() + "/reactivate")
                .header("Authorization", "Bearer " + admin.token())
                .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void rejectThenReapplyAllowed() throws Exception {
        var customer = VendorTestSupport.signupCustomer(auth, "rej");
        var app = applicationService.apply(customer.userId(), VendorTestSupport.sampleApply());
        var admin = VendorTestSupport.signupAdmin(auth, roles, "rejadm");

        mvc.perform(post("/api/v1/admin/vendors/" + app.vendorId() + "/reject")
                .header("Authorization", "Bearer " + admin.token())
                .contentType("application/json").content("{\"reason\":\"docs missing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }
}