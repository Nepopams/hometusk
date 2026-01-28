package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.RecurrenceRule;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.domain.RoutineStatus;
import com.hometusk.routines.repository.RoutineRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("RoutineController Integration Tests")
class RoutineControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RoutineRepository routineRepository;

    @Test
    @DisplayName("AC-1: Create routine as member succeeds")
    void createRoutine_asMember_succeeds() throws Exception {
        Map<String, Object> request = Map.of(
                "title",
                "Wash dishes",
                "zoneId",
                testZone.getId().toString(),
                "recurrenceRule",
                Map.of("type", "DAILY"),
                "assignmentPolicy",
                "ROUND_ROBIN");

        mockMvc.perform(post("/api/v1/households/{id}/routines", testHousehold.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.householdId").value(testHousehold.getId().toString()))
                .andExpect(jsonPath("$.createdBy.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.recurrenceRule.type").value("DAILY"));
    }

    @Test
    @DisplayName("AC-9: Non-member cannot create routine")
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
    @DisplayName("AC-7: Missing title returns 400")
    void createRoutine_missingTitle_returns400() throws Exception {
        Map<String, Object> request =
                Map.of("recurrenceRule", Map.of("type", "DAILY"), "assignmentPolicy", "ROUND_ROBIN");

        mockMvc.perform(post("/api/v1/households/{id}/routines", testHousehold.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AC-8: Missing recurrenceRule returns 400")
    void createRoutine_missingRecurrenceRule_returns400() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "Wash dishes",
                "assignmentPolicy", "ROUND_ROBIN");

        mockMvc.perform(post("/api/v1/households/{id}/routines", testHousehold.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AC-10: Invalid zone returns 400")
    void createRoutine_invalidZone_returns400() throws Exception {
        Map<String, Object> request = Map.of(
                "title",
                "Wash dishes",
                "zoneId",
                UUID.randomUUID().toString(),
                "recurrenceRule",
                Map.of("type", "DAILY"),
                "assignmentPolicy",
                "ROUND_ROBIN");

        mockMvc.perform(post("/api/v1/households/{id}/routines", testHousehold.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violations[0].rule").value("ZONE_MUST_EXIST"));
    }

    @Test
    @DisplayName("AC-2: List routines excludes deleted by default")
    void listRoutines_filtersDeletedByDefault() throws Exception {
        saveRoutine("Active Routine", RoutineStatus.ACTIVE, AssignmentPolicy.MANUAL);
        saveRoutine("Deleted Routine", RoutineStatus.DELETED, AssignmentPolicy.MANUAL);

        mockMvc.perform(get("/api/v1/households/{id}/routines", testHousehold.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Active Routine"));
    }

    @Test
    @DisplayName("AC-3: Get routine returns full object")
    void getRoutine_returnsFullObject() throws Exception {
        Routine routine = saveRoutine("Weekly Routine", RoutineStatus.ACTIVE, AssignmentPolicy.MANUAL);

        mockMvc.perform(get("/api/v1/households/{id}/routines/{routineId}", testHousehold.getId(), routine.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(routine.getId().toString()))
                .andExpect(jsonPath("$.title").value("Weekly Routine"))
                .andExpect(jsonPath("$.assignmentPolicy").value("MANUAL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdBy.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.recurrenceRule.type").value("DAILY"));
    }

    @Test
    @DisplayName("AC-4: PATCH updates only provided fields")
    void updateRoutine_partialUpdate_works() throws Exception {
        Routine routine = saveRoutine("Old Title", RoutineStatus.ACTIVE, AssignmentPolicy.MANUAL);
        Instant beforeUpdate = routine.getUpdatedAt();

        Map<String, Object> request = Map.of("title", "New Title");

        mockMvc.perform(patch("/api/v1/households/{id}/routines/{routineId}", testHousehold.getId(), routine.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));

        Routine updated = routineRepository.findById(routine.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("AC-5: DELETE soft deletes routine")
    void deleteRoutine_softDeletes() throws Exception {
        Routine routine = saveRoutine("To Delete", RoutineStatus.ACTIVE, AssignmentPolicy.MANUAL);

        mockMvc.perform(delete("/api/v1/households/{id}/routines/{routineId}", testHousehold.getId(), routine.getId())
                        .with(jwt()))
                .andExpect(status().isNoContent());

        Routine deleted = routineRepository.findById(routine.getId()).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(RoutineStatus.DELETED);
    }

    private Routine saveRoutine(String title, RoutineStatus status, AssignmentPolicy policy) throws Exception {
        String ruleJson = objectMapper.writeValueAsString(new RecurrenceRule.Daily());
        Routine routine = new Routine(testHousehold, title, ruleJson, policy, testUser);
        routine.setZone(testZone);
        if (status == RoutineStatus.DELETED) {
            routine.softDelete();
        }
        return routineRepository.save(routine);
    }
}
