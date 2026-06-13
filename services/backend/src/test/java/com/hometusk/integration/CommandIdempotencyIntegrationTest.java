package com.hometusk.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.tasks.repository.TaskRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("Command Idempotency Integration Tests")
class CommandIdempotencyIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @DisplayName("Same Idempotency-Key returns stored response and avoids duplicate actions")
    void sameKey_returnsStoredResponse() throws Exception {
        String idempotencyKey = "idem_key_1";
        String correlationId = randomCorrelationId();

        var request = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Clean kitchen"),
                "source",
                "api");

        MvcResult first = mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .header("X-Correlation-ID", correlationId)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executed"))
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .header("X-Correlation-ID", correlationId)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executed"))
                .andReturn();

        assertThat(second.getResponse().getContentAsString())
                .isEqualTo(first.getResponse().getContentAsString());

        var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
        assertThat(tasks).hasSize(1);
    }

    @Test
    @DisplayName("Same Idempotency-Key with command attributes returns stored response")
    void sameKeyWithCommandAttributes_returnsStoredResponse() throws Exception {
        String idempotencyKey = "idem_key_attrs_1";
        String dueDate = Instant.now().plus(1, ChronoUnit.DAYS).toString();

        var request = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Clean with due date"),
                "dueDate",
                dueDate,
                "source",
                "api");

        MvcResult first = mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executed"))
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executed"))
                .andReturn();

        assertThat(second.getResponse().getContentAsString())
                .isEqualTo(first.getResponse().getContentAsString());

        var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
        assertThat(tasks).hasSize(1);
    }

    @Test
    @DisplayName("Same Idempotency-Key with different command attribute returns 409")
    void sameKeyDifferentCommandAttribute_conflict() throws Exception {
        String idempotencyKey = "idem_key_attrs_2";

        var request = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Clean with due date"),
                "dueDate",
                Instant.now().plus(1, ChronoUnit.DAYS).toString(),
                "source",
                "api");

        var changedRequest = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Clean with due date"),
                "dueDate",
                Instant.now().plus(2, ChronoUnit.DAYS).toString(),
                "source",
                "api");

        mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changedRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_CONFLICT"));

        var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
        assertThat(tasks).hasSize(1);
    }

    @Test
    @DisplayName("Same Idempotency-Key with scheduled command returns stored response")
    void sameKeyWithScheduledCommand_returnsStoredResponse() throws Exception {
        String idempotencyKey = "idem_key_schedule_1";
        String scheduleAt = Instant.now().plus(1, ChronoUnit.HOURS).toString();

        var request = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Clean later"),
                "scheduleAt",
                scheduleAt,
                "source",
                "api");

        MvcResult first = mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("scheduled"))
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("scheduled"))
                .andReturn();

        assertThat(second.getResponse().getContentAsString())
                .isEqualTo(first.getResponse().getContentAsString());

        assertThat(taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("Same Idempotency-Key with different scheduleAt returns 409")
    void sameKeyDifferentScheduleAt_conflict() throws Exception {
        String idempotencyKey = "idem_key_schedule_2";

        var request = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Clean later"),
                "scheduleAt",
                Instant.now().plus(1, ChronoUnit.HOURS).toString(),
                "source",
                "api");

        var changedRequest = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Clean later"),
                "scheduleAt",
                Instant.now().plus(2, ChronoUnit.HOURS).toString(),
                "source",
                "api");

        mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("scheduled"));

        mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changedRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_CONFLICT"));

        assertThat(taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("Same Idempotency-Key with different payload returns 409")
    void sameKeyDifferentPayload_conflict() throws Exception {
        String idempotencyKey = "idem_key_2";

        var request = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Clean kitchen"),
                "source",
                "api");

        var changedRequest = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Clean bathroom"),
                "source",
                "api");

        mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changedRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_CONFLICT"));

        var tasks = taskRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());
        assertThat(tasks).hasSize(1);
    }
}
