package com.hometusk.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("MVP Journey Integration Test")
class MvpJourneyIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Create household -> create zone -> create task -> list tasks")
    void endToEndJourney() throws Exception {
        var householdRequest = Map.of("name", "Web MVP Household");

        var householdResult = mockMvc.perform(post("/api/v1/households")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(householdRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        UUID householdId = UUID.fromString(objectMapper
                .readTree(householdResult.getResponse().getContentAsString())
                .get("id")
                .asText());

        var zoneRequest = Map.of("name", "Entry");
        var zoneResult = mockMvc.perform(post("/api/v1/households/{id}/zones", householdId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zoneRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        UUID zoneId = UUID.fromString(objectMapper
                .readTree(zoneResult.getResponse().getContentAsString())
                .get("id")
                .asText());

        var commandRequest = Map.of(
                "householdId",
                householdId.toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Sweep entryway", "zoneId", zoneId.toString()),
                "source",
                "web");

        mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commandRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executed"))
                .andExpect(jsonPath("$.result.taskId").exists());

        mockMvc.perform(get("/api/v1/households/{id}/tasks", householdId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Sweep entryway"));
    }
}
