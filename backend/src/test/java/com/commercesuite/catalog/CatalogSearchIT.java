package com.commercesuite.catalog;

import com.commercesuite.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class CatalogSearchIT extends AbstractIT {
    @Autowired MockMvc mvc;

    @Test
    void publicCatalogIsReachableWithoutAuth() throws Exception {
        mvc.perform(get("/api/v1/catalog/products")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalog/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalog/brands")).andExpect(status().isOk());
    }
}