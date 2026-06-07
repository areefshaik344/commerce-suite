package com.commercesuite.rbac;

import com.commercesuite.AbstractIT;
import com.commercesuite.auth.dto.SignupRequest;
import com.commercesuite.auth.service.AuthService;
import com.commercesuite.rbac.entity.AppRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class RBACPermissionIT extends AbstractIT {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;

    @Test
    void customerCanReadOwnProfileAndAddresses() throws Exception {
        String email = "rbac+" + UUID.randomUUID() + "@example.com";
        var res = auth.signup(new SignupRequest(email, "Str0ng!Pwd", "RBAC", null, AppRole.CUSTOMER),
                "ua", "127.0.0.1");
        String token = res.tokens().accessToken();

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/me/addresses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void invalidTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotMutateAddressWithBadPayload() throws Exception {
        String email = "rbac2+" + UUID.randomUUID() + "@example.com";
        var res = auth.signup(new SignupRequest(email, "Str0ng!Pwd", "RBAC2", null, AppRole.CUSTOMER),
                "ua", "127.0.0.1");
        String token = res.tokens().accessToken();

        mvc.perform(post("/api/v1/me/addresses")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("VALIDATION_FAILED"));
    }
}
