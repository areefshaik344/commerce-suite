package com.commercesuite.auth;

import com.commercesuite.AbstractIT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AuthControllerIT extends AbstractIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void signupLoginRefreshAndMe() throws Exception {
        // Signup
        String signupBody = """
            {"email":"alice@example.com","password":"Str0ng!Pwd","fullName":"Alice","requestedRole":"CUSTOMER"}
            """;
        String resp = mvc.perform(post("/api/v1/auth/signup")
                .contentType("application/json").content(signupBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        JsonNode tokens = mapper.readTree(resp).get("data");
        String refresh = tokens.get("refreshToken").asText();

        // Login
        String loginBody = "{\"email\":\"alice@example.com\",\"password\":\"Str0ng!Pwd\"}";
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));

        // Refresh
        String refreshBody = mapper.writeValueAsString(java.util.Map.of("refreshToken", refresh));
        String refreshed = mvc.perform(post("/api/v1/auth/refresh")
                .contentType("application/json").content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        String newAccess = mapper.readTree(refreshed).get("data").get("accessToken").asText();

        // /me with new access token
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.roles").isArray());
    }

    @Test
    void rejectsAdminSelfRegistration() throws Exception {
        String body = """
            {"email":"hax@example.com","password":"Str0ng!Pwd","requestedRole":"ADMIN"}
            """;
        mvc.perform(post("/api/v1/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsWeakPassword() throws Exception {
        String body = """
            {"email":"weak@example.com","password":"weakpass","requestedRole":"CUSTOMER"}
            """;
        mvc.perform(post("/api/v1/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("WEAK_PASSWORD"));
    }

    @Test
    void anonymousMeIsUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }
}
