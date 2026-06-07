package com.commercesuite.vendor;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class VendorPermissionIT extends AbstractIT {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;

    @Test
    void customerCannotHitAdminEndpoints() throws Exception {
        var u = VendorTestSupport.signupCustomer(auth, "permcust");
        mvc.perform(get("/api/v1/admin/vendors").header("Authorization", "Bearer " + u.token()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/vendors/00000000-0000-0000-0000-000000000000/approve")
                .header("Authorization", "Bearer " + u.token())
                .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerWithoutVendorRowGetsNotFoundOnMyProfile() throws Exception {
        var u = VendorTestSupport.signupCustomer(auth, "permnope");
        mvc.perform(get("/api/v1/vendors/me/profile").header("Authorization", "Bearer " + u.token()))
                .andExpect(status().isNotFound());
    }
}