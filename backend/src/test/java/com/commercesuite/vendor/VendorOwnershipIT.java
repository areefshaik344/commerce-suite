package com.commercesuite.vendor;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.vendor.service.VendorApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class VendorOwnershipIT extends AbstractIT {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired VendorApplicationService applicationService;

    @Test
    void otherCustomerCannotReadAnotherVendorMe() throws Exception {
        var owner = VendorTestSupport.signupCustomer(auth, "owner");
        applicationService.apply(owner.userId(), VendorTestSupport.sampleApply());

        var stranger = VendorTestSupport.signupCustomer(auth, "stranger");

        // Stranger has no Vendor row -> 404, never sees owner's data.
        mvc.perform(get("/api/v1/vendors/me")
                .header("Authorization", "Bearer " + stranger.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousCannotApply() throws Exception {
        mvc.perform(get("/api/v1/vendors/me")).andExpect(status().isUnauthorized());
    }
}