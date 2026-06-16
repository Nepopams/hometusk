package com.hometusk.commands.pipeline.decision.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.commands.domain.CommandType;
import com.hometusk.commands.domain.DecisionSource;
import com.hometusk.commands.pipeline.decision.DecisionContext;
import com.hometusk.commands.pipeline.decision.DecisionResult;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiPlatformDecisionAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiDecisionResponseMapper mapper = new AiDecisionResponseMapper(objectMapper);
    private final AiResponseSchemaValidator schemaValidator = new AiResponseSchemaValidator(objectMapper);

    @Test
    void requestUsesUpstreamShapeAndHouseholdContext() {
        UUID commandId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID householdId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        DecisionContext context = DecisionContext.builder()
                .commandId(commandId)
                .correlationId(UUID.randomUUID())
                .commandType(CommandType.CREATE_TASK)
                .requesterId(requesterId)
                .householdId(householdId)
                .payload(Map.of("title", "Buy milk"))
                .householdContext(Map.of(
                        "members",
                        List.of(Map.of("id", memberId.toString(), "name", "Vadim", "workload_score", 0.2)),
                        "zones",
                        List.of(Map.of("id", zoneId.toString(), "name", "Kitchen")),
                        "shopping_lists",
                        List.of(Map.of("id", listId.toString(), "name", "Groceries")),
                        "default_list_id",
                        listId.toString()))
                .build();

        AiDecisionRequest request = AiDecisionRequest.from(context);

        assertThat(request.commandId()).isEqualTo(commandId.toString());
        assertThat(request.userId()).isEqualTo(requesterId.toString());
        assertThat(request.text()).isEqualTo("Buy milk");
        assertThat(request.capabilities())
                .contains("start_job", "propose_create_task", "propose_add_shopping_item", "reject")
                .doesNotContain("confirm");

        Map<String, Object> household = nestedMap(request.context(), "household");
        assertThat(household).containsEntry("household_id", householdId.toString());
        assertThat(nestedList(household, "members").get(0))
                .containsEntry("user_id", memberId.toString())
                .containsEntry("display_name", "Vadim")
                .containsEntry("workload_score", 0.2);
        assertThat(nestedList(household, "zones").get(0)).containsEntry("zone_id", zoneId.toString());
        assertThat(nestedList(household, "shopping_lists").get(0)).containsEntry("list_id", listId.toString());

        Map<String, Object> defaults = nestedMap(request.context(), "defaults");
        assertThat(defaults)
                .containsEntry("default_assignee_id", requesterId.toString())
                .containsEntry("default_list_id", listId.toString());
    }

    @Test
    void mapsStartJobProposedActionsToExecutableActions() {
        UUID decisionId = UUID.randomUUID();
        AiDecisionResponse response = new AiDecisionResponse(
                decisionId.toString(),
                "cmd-1",
                "ok",
                "start_job",
                "execute",
                new BigDecimal("0.91"),
                Map.of(
                        "job_id",
                        "job-1",
                        "job_type",
                        "create_task",
                        "proposed_actions",
                        List.of(
                                Map.of(
                                        "action",
                                        "propose_create_task",
                                        "payload",
                                        Map.of(
                                                "task",
                                                Map.of(
                                                        "title",
                                                        "Clean kitchen",
                                                        "assignee_id",
                                                        "user-1",
                                                        "zone_id",
                                                        "zone-1",
                                                        "due",
                                                        "2026-06-15T10:00:00Z"))),
                                Map.of(
                                        "action",
                                        "propose_add_shopping_item",
                                        "payload",
                                        Map.of(
                                                "item",
                                                Map.of(
                                                        "name",
                                                        "Milk",
                                                        "quantity",
                                                        "2",
                                                        "unit",
                                                        "l",
                                                        "list_id",
                                                        "list-1"))))),
                "Accepted.",
                "trace-1",
                "1.0.0",
                "test",
                "2026-06-14T00:00:00Z");

        assertThat(schemaValidator.validate(response).valid()).isTrue();

        DecisionResult result = mapper.toDecisionResult(response);

        assertThat(result).isInstanceOf(DecisionResult.StartJob.class);
        DecisionResult.StartJob startJob = (DecisionResult.StartJob) result;
        assertThat(startJob.source()).isEqualTo(DecisionSource.AI_PLATFORM);
        assertThat(startJob.externalDecisionId()).isEqualTo(decisionId);
        assertThat(startJob.actions()).hasSize(2);
        assertThat(startJob.actions().get(0).actionType()).isEqualTo("create_task");
        assertThat(startJob.actions().get(0).parameters())
                .containsEntry("title", "Clean kitchen")
                .containsEntry("assigneeId", "user-1")
                .containsEntry("zoneId", "zone-1")
                .containsEntry("deadline", "2026-06-15T10:00:00Z");
        assertThat(startJob.actions().get(1).actionType()).isEqualTo("add_shopping_item");
        assertThat(startJob.actions().get(1).parameters())
                .containsEntry("name", "Milk")
                .containsEntry("quantity", "2")
                .containsEntry("unit", "l")
                .containsEntry("listId", "list-1");
    }

    @Test
    void mapsTopLevelClarify() {
        AiDecisionResponse response = new AiDecisionResponse(
                "dec-clarify",
                "cmd-1",
                "clarify",
                "clarify",
                "clarify",
                new BigDecimal("0.42"),
                Map.of("question", "Which room?", "missing_fields", List.of("zoneId"), "options", List.of("Kitchen")),
                "More details required.",
                "trace-clarify",
                "1.0.0",
                "test",
                "2026-06-14T00:00:00Z");

        assertThat(schemaValidator.validate(response).valid()).isTrue();

        DecisionResult result = mapper.toDecisionResult(response);

        assertThat(result).isInstanceOf(DecisionResult.Clarify.class);
        DecisionResult.Clarify clarify = (DecisionResult.Clarify) result;
        assertThat(clarify.externalDecisionId()).isNull();
        assertThat(clarify.question()).isEqualTo("Which room?");
        assertThat(clarify.requiredFields()).containsExactly("zoneId");
        assertThat(clarify.suggestions()).containsKey("options");
    }

    @Test
    void mapsProviderRejectToSafeReject() {
        UUID decisionId = UUID.randomUUID();
        AiDecisionResponse response = new AiDecisionResponse(
                decisionId.toString(),
                "cmd-1",
                "error",
                "reject",
                "reject",
                new BigDecimal("0.21"),
                Map.of(
                        "code",
                        "unsupported_or_unsafe_command",
                        "reason",
                        "Outside the supported command corridor.",
                        "ui_message",
                        "I cannot safely handle this request.",
                        "details",
                        Map.of("category", "unsupported")),
                "Unsupported command is rejected without proposed mutation.",
                "trace-reject",
                "2.1.0",
                "mvp1-graph-0.1",
                "2026-06-15T00:00:00Z");

        assertThat(schemaValidator.validate(response).valid()).isTrue();

        DecisionResult result = mapper.toDecisionResult(response, "{\"action\":\"reject\"}");

        assertThat(result).isInstanceOf(DecisionResult.Reject.class);
        DecisionResult.Reject reject = (DecisionResult.Reject) result;
        assertThat(reject.externalDecisionId()).isEqualTo(decisionId);
        assertThat(reject.errorCode()).isEqualTo("unsupported_or_unsafe_command");
        assertThat(reject.reason()).isEqualTo("I cannot safely handle this request.");
        assertThat(reject.rawPayload()).isEqualTo("{\"action\":\"reject\"}");
    }

    @Test
    void mapsProviderConfirmToUnsupportedReject() {
        UUID decisionId = UUID.randomUUID();
        AiDecisionResponse response = new AiDecisionResponse(
                decisionId.toString(),
                "cmd-1",
                "ok",
                "confirm",
                "confirm",
                new BigDecimal("0.73"),
                Map.of(
                        "confirmation_id",
                        "conf-1",
                        "summary",
                        "Create a task for another household member.",
                        "reasons",
                        List.of("Non-requester assignment requires confirmation."),
                        "proposed_actions",
                        List.of(Map.of(
                                "action",
                                "propose_create_task",
                                "payload",
                                Map.of("task", Map.of("title", "Clean kitchen")))),
                        "expires_at",
                        "2026-06-15T01:00:00Z",
                        "ui_message",
                        "Please confirm before I do this."),
                "Confirmation required.",
                "trace-confirm",
                "2.1.0",
                "mvp1-graph-0.1",
                "2026-06-15T00:00:00Z");

        assertThat(schemaValidator.validate(response).valid()).isTrue();

        DecisionResult result = mapper.toDecisionResult(response, "{\"action\":\"confirm\"}");

        assertThat(result).isInstanceOf(DecisionResult.Reject.class);
        DecisionResult.Reject reject = (DecisionResult.Reject) result;
        assertThat(reject.externalDecisionId()).isEqualTo(decisionId);
        assertThat(reject.errorCode()).isEqualTo("AI_CONFIRMATION_UNSUPPORTED");
        assertThat(reject.reason()).isEqualTo("Please confirm before I do this.");
        assertThat(reject.rawPayload()).isEqualTo("{\"action\":\"confirm\"}");
    }

    @Test
    void rejectsLegacyResponseShapeBeforeMapping() {
        String legacyResponse =
                """
                {
                  "id": "legacy",
                  "type": "start_job",
                  "confidence": 0.9,
                  "actions": []
                }
                """;

        assertThat(schemaValidator.validateRaw(legacyResponse).valid()).isFalse();
    }

    @Test
    void validatesProvider21Fixtures() throws Exception {
        for (String fixture : List.of(
                "decision-execute-create-task.json",
                "decision-execute-shopping-items.json",
                "decision-clarify.json",
                "decision-reject.json",
                "decision-confirm.json")) {
            assertThat(schemaValidator.validateRaw(readFixture(fixture)).valid())
                    .as(fixture)
                    .isTrue();
        }

        assertThat(schemaValidator
                        .validateRaw(readFixture("decision-invalid-unknown-action.json"))
                        .valid())
                .isFalse();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        return (Map<String, Object>) source.get(key);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> nestedList(Map<String, Object> source, String key) {
        return (List<Map<String, Object>>) source.get(key);
    }

    private String readFixture(String fileName) throws Exception {
        try (var stream = getClass().getResourceAsStream("/ai-platform/v2.1/" + fileName)) {
            assertThat(stream).as(fileName).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
