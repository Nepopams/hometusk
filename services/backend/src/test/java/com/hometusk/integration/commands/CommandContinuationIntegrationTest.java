package com.hometusk.integration.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.commands.repository.DecisionLogRepository;
import com.hometusk.integration.IntegrationTestBase;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("Command Continuation Integration Tests")
class CommandContinuationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DecisionLogRepository decisionLogRepository;

    @Test
    @DisplayName("Continue NEEDS_INPUT command and execute successfully")
    void continueCommand_HappyPath() throws Exception {
        UUID commandId = createNeedsInputCommand();

        var continueRequest = Map.of("additionalInput", Map.of("deadline", validDeadline()));

        mockMvc.perform(post("/api/v1/commands/{id}/continue", commandId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(continueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executed"))
                .andExpect(jsonPath("$.result.taskId").exists());
    }

    @Test
    @DisplayName("Continue EXECUTED command returns 400")
    void continueCommand_InvalidState_Returns400() throws Exception {
        UUID commandId = createExecutedCommand();

        var continueRequest = Map.of("additionalInput", Map.of("deadline", validDeadline()));

        mockMvc.perform(post("/api/v1/commands/{id}/continue", commandId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(continueRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMAND_NOT_CONTINUABLE"));
    }

    @Test
    @DisplayName("Continue non-existent command returns 404")
    void continueCommand_NotFound_Returns404() throws Exception {
        var continueRequest = Map.of("additionalInput", Map.of("deadline", validDeadline()));

        mockMvc.perform(post("/api/v1/commands/{id}/continue", UUID.randomUUID())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(continueRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMAND_NOT_FOUND"));
    }

    @Test
    @DisplayName("Continue command from different user returns 403")
    void continueCommand_DifferentUser_Returns403() throws Exception {
        UUID commandId = createNeedsInputCommand();

        var continueRequest = Map.of("additionalInput", Map.of("deadline", validDeadline()));

        mockMvc.perform(post("/api/v1/commands/{id}/continue", commandId)
                        .with(jwtForUser(testUser2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(continueRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Continuation writes DecisionLog entry with correlationId")
    void continueCommand_DecisionLogUpdated() throws Exception {
        UUID commandId = createNeedsInputCommand();
        String correlationId = randomCorrelationId();

        var continueRequest = Map.of("additionalInput", Map.of("deadline", validDeadline()));

        mockMvc.perform(post("/api/v1/commands/{id}/continue", commandId)
                        .with(jwt())
                        .header("X-Correlation-ID", correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(continueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executed"));

        assertThat(decisionLogRepository.findByCorrelationId(UUID.fromString(correlationId)))
                .isPresent();
    }

    private UUID createNeedsInputCommand() throws Exception {
        var commandRequest = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                Map.of(
                        "title", "Deep clean kitchen",
                        "zoneId", testZone.getId().toString(),
                        "deadline", farFutureDeadline()),
                "source",
                "api");

        MvcResult result = mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commandRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("needs_input"))
                .andReturn();

        return extractCommandId(result);
    }

    private UUID createExecutedCommand() throws Exception {
        var commandRequest = Map.of(
                "householdId",
                testHousehold.getId().toString(),
                "type",
                "create_task",
                "payload",
                Map.of("title", "Wipe counters", "zoneId", testZone.getId().toString()),
                "source",
                "api");

        MvcResult result = mockMvc.perform(post("/api/v1/commands")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commandRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executed"))
                .andReturn();

        return extractCommandId(result);
    }

    private UUID extractCommandId(MvcResult result) throws Exception {
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("commandId")
                .asText());
    }

    private String farFutureDeadline() {
        return ZonedDateTime.now(ZoneOffset.UTC)
                .plusDays(400)
                .withHour(12)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toInstant()
                .toString();
    }

    private String validDeadline() {
        return ZonedDateTime.now(ZoneOffset.UTC)
                .plusDays(10)
                .withHour(12)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toInstant()
                .toString();
    }
}
