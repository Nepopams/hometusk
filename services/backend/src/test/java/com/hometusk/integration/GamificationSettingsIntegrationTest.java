package com.hometusk.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Gamification Settings Integration Tests - ST-906")
class GamificationSettingsIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("GET /settings returns defaults for new user")
    void getSettings_newUser_returnsDefaults() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/gamification/settings", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showProgressToOthers").value(true))
                .andExpect(jsonPath("$.gamificationEnabled").value(true))
                .andExpect(jsonPath("$.streakVisible").value(true));
    }

    @Test
    @DisplayName("PUT /settings updates and persists")
    void putSettings_updatesAndPersists() throws Exception {
        Map<String, Object> request = Map.of(
                "showProgressToOthers", false,
                "gamificationEnabled", false,
                "streakVisible", false);

        mockMvc.perform(put("/api/v1/households/{id}/gamification/settings", testHousehold.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showProgressToOthers").value(false))
                .andExpect(jsonPath("$.gamificationEnabled").value(false))
                .andExpect(jsonPath("$.streakVisible").value(false));

        mockMvc.perform(get("/api/v1/households/{id}/gamification/settings", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showProgressToOthers").value(false))
                .andExpect(jsonPath("$.gamificationEnabled").value(false))
                .andExpect(jsonPath("$.streakVisible").value(false));
    }

    @Test
    @DisplayName("GET /settings returns 403 for non-member")
    void getSettings_notMember_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/gamification/settings", testHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /settings returns 403 for non-member")
    void putSettings_notMember_returns403() throws Exception {
        Map<String, Object> request = Map.of(
                "showProgressToOthers", false,
                "gamificationEnabled", false,
                "streakVisible", false);

        mockMvc.perform(put("/api/v1/households/{id}/gamification/settings", testHousehold.getId())
                        .with(jwtForUser(testUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
