package com.commercesuite.vendor;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class VendorApplicationIT extends AbstractIT {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ObjectMapper mapper;

    @Test
    void customerCanApplyAndCannotDouble() throws Exception {
        var u = VendorTestSupport.signupCustomer(auth, "vapply");
        String body = mapper.writeValueAsString(VendorTestSupport.sampleApply());

        mvc.perform(post("/api/v1/vendors/apply")
                .header("Authorization", "Bearer " + u.token())
                .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));

        // Second apply -> 409
        mvc.perform(post("/api/v1/vendors/apply")
                .header("Authorization", "Bearer " + u.token())
                .contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void applyRejectsBadGstin() throws Exception {
        var u = VendorTestSupport.signupCustomer(auth, "vbad");
        String body = """
            { "legalName":"X","displayName":"Y","businessName":"X","businessType":"SOLE",
              "gstin":"BAD","pan":"ABCDE1234F","contactEmail":"a@b.com",
              "contactPhone":"+91 9876543210","registeredAddress":"addr" }
            """;
        mvc.perform(post("/api/v1/vendors/apply")
                .header("Authorization", "Bearer " + u.token())
                .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("VALIDATION_FAILED"));
    }

    @Test
    void anonymousApplyIsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/vendors/apply").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }
}