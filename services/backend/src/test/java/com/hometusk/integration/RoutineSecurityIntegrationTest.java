package com.hometusk.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.households.domain.Household;
import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.RecurrenceRule;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.repository.RoutineRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("Routine Security Integration Tests")
class RoutineSecurityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RoutineRepository routineRepository;

    private Household otherHousehold;
    private Routine routineInTestHousehold;
    private Routine routineInOtherHousehold;

    @BeforeEach
    void setUpRoutines() throws Exception {
        otherHousehold = householdRepository.save(new Household("Other Household " + UUID.randomUUID()));
        membershipRepository.save(new Membership(testUser2, otherHousehold, MembershipRole.member));

        routineInTestHousehold = saveRoutine(testHousehold, testUser, "Test Routine");
        routineInOtherHousehold = saveRoutine(otherHousehold, testUser2, "Other Routine");
    }

    @Test
    @DisplayName("AC-1: List routines returns 403 for non-member")
    void listRoutines_notMember_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/routines", testHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-2: Create routine returns 403 for non-member")
    void createRoutine_notMember_returns403() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "Wash dishes",
                "recurrenceRule", Map.of("type", "DAILY"),
                "assignmentPolicy", "ROUND_ROBIN");

        mockMvc.perform(post("/api/v1/households/{id}/routines", testHousehold.getId())
                        .with(jwtForUser(testUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-3: Get routine returns 403 for non-member")
    void getRoutine_notMember_returns403() throws Exception {
        mockMvc.perform(get(
                                "/api/v1/households/{id}/routines/{routineId}",
                                testHousehold.getId(),
                                routineInTestHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-4: Update routine returns 403 for non-member")
    void updateRoutine_notMember_returns403() throws Exception {
        Map<String, Object> request = Map.of("title", "New Title");

        mockMvc.perform(patch(
                                "/api/v1/households/{id}/routines/{routineId}",
                                testHousehold.getId(),
                                routineInTestHousehold.getId())
                        .with(jwtForUser(testUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-5: Delete routine returns 403 for non-member")
    void deleteRoutine_notMember_returns403() throws Exception {
        mockMvc.perform(delete(
                                "/api/v1/households/{id}/routines/{routineId}",
                                testHousehold.getId(),
                                routineInTestHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-6: Pause routine returns 403 for non-member")
    void pauseRoutine_notMember_returns403() throws Exception {
        mockMvc.perform(post(
                                "/api/v1/households/{id}/routines/{routineId}/pause",
                                testHousehold.getId(),
                                routineInTestHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-6: Resume routine returns 403 for non-member")
    void resumeRoutine_notMember_returns403() throws Exception {
        mockMvc.perform(post(
                                "/api/v1/households/{id}/routines/{routineId}/resume",
                                testHousehold.getId(),
                                routineInTestHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-7: Upcoming returns 403 for non-member")
    void upcoming_notMember_returns403() throws Exception {
        mockMvc.perform(get(
                                "/api/v1/households/{id}/routines/{routineId}/upcoming",
                                testHousehold.getId(),
                                routineInTestHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-8: No cross-household routine listing")
    void listRoutines_crossHousehold_noLeaks() throws Exception {
        mockMvc.perform(get("/api/v1/households/{id}/routines", otherHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(routineInOtherHousehold.getId().toString()));
    }

    @Test
    @DisplayName("AC-9: Routine ID in wrong household returns 404")
    void getRoutine_wrongHousehold_returns404() throws Exception {
        mockMvc.perform(get(
                                "/api/v1/households/{id}/routines/{routineId}",
                                otherHousehold.getId(),
                                routineInTestHousehold.getId())
                        .with(jwtForUser(testUser2)))
                .andExpect(status().isNotFound());
    }

    private Routine saveRoutine(Household household, User creator, String title) throws Exception {
        String ruleJson = objectMapper.writeValueAsString(new RecurrenceRule.Daily());
        Routine routine = new Routine(household, title, ruleJson, AssignmentPolicy.MANUAL, creator);
        return routineRepository.save(routine);
    }
}
