package com.hometusk.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("Gamification Security Integration Tests - S08")
class GamificationSecurityIntegrationTest extends IntegrationTestBase {

    private void addMember(User user, Household household, MembershipRole role) {
        Membership membership = new Membership(user, household, role);
        membershipRepository.save(membership);
    }

    private UUID createTaskAndComplete(UUID householdId, User actor, UUID assigneeId) throws Exception {
        Map<String, Object> createCommand = Map.of(
                "householdId",
                householdId.toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Test task " + UUID.randomUUID(), "assigneeId", assigneeId.toString()),
                "source",
                "web");

        MvcResult createResult = mockMvc.perform(post("/api/v1/commands")
                        .with(jwtForUser(actor))
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
                        .with(jwtForUser(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeCommand)))
                .andExpect(status().isOk());

        return taskId;
    }

    @Test
    @DisplayName("AC-1: Returns 403 for non-members")
    void getProgress_notMember_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-2: No cross-household data leak")
    void getProgress_differentHousehold_noDataLeak() throws Exception {
        UUID taskH1 = createTaskAndComplete(testHousehold.getId(), testUser, testUser.getId());

        Household household2 = householdRepository.save(new Household("Household Two"));
        addMember(testUser2, household2, MembershipRole.admin);
        createTaskAndComplete(household2.getId(), testUser2, testUser2.getId());

        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(10))
                .andExpect(jsonPath("$.householdTotalPoints").value(10))
                .andExpect(jsonPath("$.householdTotalTasks").value(1))
                .andExpect(jsonPath("$.recentActivity[0].taskId").value(taskH1.toString()));
    }

    @Test
    @DisplayName("AC-3: IDOR attempt returns 403 (not 404)")
    void getProgress_idorAttempt_returns403() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", randomId)
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-4: User sees only own progress details")
    void getProgress_userSeesOnlyOwnProgress() throws Exception {
        addMember(testUser2, testHousehold, MembershipRole.member);

        UUID taskU1 = createTaskAndComplete(testHousehold.getId(), testUser, testUser.getId());
        createTaskAndComplete(testHousehold.getId(), testUser2, testUser2.getId());

        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.totalPoints").value(10))
                .andExpect(jsonPath("$.pointsThisWeek").value(10))
                .andExpect(jsonPath("$.recentActivity.length()").value(1))
                .andExpect(jsonPath("$.recentActivity[0].taskId").value(taskU1.toString()))
                .andExpect(jsonPath("$.householdTotalPoints").value(20));
    }

    @Test
    @DisplayName("AC-5: Household aggregate includes all members")
    void getProgress_householdAggregateIncludesAllMembers() throws Exception {
        addMember(testUser2, testHousehold, MembershipRole.member);

        createTaskAndComplete(testHousehold.getId(), testUser, testUser.getId());
        createTaskAndComplete(testHousehold.getId(), testUser2, testUser2.getId());

        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                        .with(jwt()))
                .andExpect(jsonPath("$.householdTotalPoints").value(20))
                .andExpect(jsonPath("$.householdTotalTasks").value(2));

        mockMvc.perform(get("/api/v1/households/{id}/gamification/progress", testHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(jsonPath("$.householdTotalPoints").value(20))
                .andExpect(jsonPath("$.householdTotalTasks").value(2));
    }
}
