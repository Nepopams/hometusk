package com.hometusk.integration;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("GamificationController Integration Tests")
class GamificationControllerIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Should return progress for household member")
    void getProgress_asMember_returnsProgress() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.totalPoints").value(0))
                .andExpect(jsonPath("$.pointsThisWeek").value(0))
                .andExpect(jsonPath("$.householdTotalPoints").value(0))
                .andExpect(jsonPath("$.householdTotalTasks").value(0));
    }

    @Test
    @DisplayName("Should reject progress for non-member")
    void getProgress_asNonMember_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should show points after completing a task")
    void getProgress_afterTaskComplete_showsPoints() throws Exception {
        UUID taskId = createTaskAndComplete(testHousehold.getId(), testUser.getId());

        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(10))
                .andExpect(jsonPath("$.pointsThisWeek").value(10))
                .andExpect(jsonPath("$.householdTotalPoints").value(10))
                .andExpect(jsonPath("$.householdTotalTasks").value(1))
                .andExpect(jsonPath("$.recentActivity.length()").value(1))
                .andExpect(jsonPath("$.recentActivity[0].taskId").value(taskId.toString()))
                .andExpect(jsonPath("$.recentActivity[0].reason").value("task_completed"))
                .andExpect(jsonPath("$.earnedBadges[0].code").value("FIRST_TASK"));
    }

    @Test
    @DisplayName("Should return badge catalog")
    void getBadges_asMember_returnsCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/gamification/badges", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.badges.length()").value(5));
    }

    @Test
    @DisplayName("Should mark earned badge in catalog")
    void getBadges_showsEarnedStatus() throws Exception {
        createTaskAndComplete(testHousehold.getId(), testUser.getId());

        mockMvc.perform(get("/api/v1/households/{id}/gamification/badges", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.badges[?(@.code == 'FIRST_TASK')].earned").value(contains(true)));
    }

    private UUID createTaskAndComplete(UUID householdId, UUID assigneeId) throws Exception {
        Map<String, Object> createCommand = Map.of(
                "householdId",
                householdId.toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Mop the floor", "assigneeId", assigneeId.toString()),
                "source",
                "web");

        MvcResult createResult = mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCommand)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode createResponse =
                objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID taskId = UUID.fromString(createResponse.get("result").get("taskId").asText());

        Map<String, Object> completeCommand = Map.of(
                "householdId",
                householdId.toString(),
                "type",
                "complete_task",
                "payload",
                Map.of("taskId", taskId.toString()),
                "source",
                "web");

        mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeCommand)))
                .andExpect(status().isOk());

        return taskId;
    }
}
