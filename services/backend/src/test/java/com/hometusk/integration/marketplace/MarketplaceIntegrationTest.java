package com.hometusk.integration.marketplace;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;

class MarketplaceIntegrationTest extends IntegrationTestBase {

    @Test
    void getMarketplaceTemplates_returnsConfiguredTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("ozon")))
                .andExpect(jsonPath("$[0].name", is("Ozon")))
                .andExpect(jsonPath("$[0].urlTemplate").exists())
                .andExpect(jsonPath("$[1].id", is("yandex_market")));
    }

    @Test
    void getMarketplaceTemplates_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace-templates")).andExpect(status().isOk());
    }

    @Test
    void getMarketplaceTemplates_responseStructure_matchesContract() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isString())
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].urlTemplate").isString())
                .andExpect(jsonPath("$[0].iconUrl").exists());
    }
}
